package com.pm.infra;

import com.pm.infra.constructs.EcsClusterConstruct;
import com.pm.infra.constructs.MskConstruct;
import com.pm.infra.constructs.RdsConstruct;
import com.pm.infra.constructs.ServiceConstruct;
import com.pm.infra.constructs.VpcConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

/** PatientManagementStack — The core infrastructure definition. */
public class PatientManagementStack extends Stack {

    public PatientManagementStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Step 1: Provision the Network (VPC)
        VpcConstruct vpcConstruct = new VpcConstruct(this, "VpcConstruct");

        // Step 2: Provision Databases (RDS)
        RdsConstruct authDb =
                new RdsConstruct(this, "AuthServiceDb", vpcConstruct.getVpc(), "authdb");
        RdsConstruct patientDb =
                new RdsConstruct(this, "PatientServiceDb", vpcConstruct.getVpc(), "patientdb");

        // Step 3: Provision Message Broker (Kafka/MSK)
        // LocalStack's MSK CloudFormation resource can remain in CREATING indefinitely.
        // For local deploys, services use LocalStack's Kafka bootstrap endpoints
        // directly.
        boolean localstack = isLocalStackDeploy();
        MskConstruct mskCluster =
                localstack ? null : new MskConstruct(this, "MskConstruct", vpcConstruct.getVpc());

        // Step 4: Provision Container Orchestrator (ECS Cluster)
        EcsClusterConstruct ecsCluster =
                new EcsClusterConstruct(this, "EcsClusterConstruct", vpcConstruct.getVpc());

        // Constants for environment configurations
        String kafkaEnv = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        String kafkaBootstrapServers =
                (kafkaEnv != null && !kafkaEnv.isBlank())
                        ? kafkaEnv
                        : (localstack
                                ? "host.docker.internal:4510, host.docker.internal:4511,"
                                        + " host.docker.internal:4512"
                                : "localhost.localstack.cloud:4510,"
                                        + " localhost.localstack.cloud:4511,"
                                        + " localhost.localstack.cloud:4512");

        String billingServiceAddress =
                localstack ? "host.docker.internal" : "billing-service.pm.local";
        String authServiceUrl =
                localstack
                        ? "http://host.docker.internal:4005"
                        : "http://auth-service.pm.local:4005";
        String patientServiceUrl =
                localstack
                        ? "http://host.docker.internal:4000"
                        : "http://patient-service.pm.local:4000";

        // Step 5: Provision Microservices (Fargate)

        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "admin_password");
        String jwtSecret =
                System.getenv()
                        .getOrDefault("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM=");

        // 5a. Auth Service
        Map<String, String> authEnv = new HashMap<>();
        authEnv.put(
                "SPRING_DATASOURCE_URL",
                String.format(
                        "jdbc:postgresql://%s:%s/authdb",
                        authDb.getDatabaseInstance().getDbInstanceEndpointAddress(),
                        authDb.getDatabaseInstance().getDbInstanceEndpointPort()));
        authEnv.put("SPRING_DATASOURCE_USERNAME", "admin_user");
        authEnv.put("SPRING_DATASOURCE_PASSWORD", dbPassword);
        authEnv.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
        authEnv.put("JWT_SECRET", jwtSecret);

        ServiceConstruct authService =
                new ServiceConstruct(
                        this,
                        "AuthService",
                        ecsCluster.getCluster(),
                        "auth-service",
                        List.of(4005),
                        authEnv);
        authService.getNode().addDependency(authDb.getDatabaseInstance());

        // 5b. Billing Service
        ServiceConstruct billingService =
                new ServiceConstruct(
                        this,
                        "BillingService",
                        ecsCluster.getCluster(),
                        "billing-service",
                        List.of(4001, 9001),
                        Map.of());

        // 5c. Analytics Service
        ServiceConstruct analyticsService =
                new ServiceConstruct(
                        this,
                        "AnalyticsService",
                        ecsCluster.getCluster(),
                        "analytics-service",
                        List.of(4002),
                        Map.of("SPRING_KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrapServers));
        if (mskCluster != null) {
            analyticsService.getNode().addDependency(mskCluster.getMskCluster());
        }

        // 5d. Patient Service
        Map<String, String> patientEnv = new HashMap<>();
        patientEnv.put(
                "SPRING_DATASOURCE_URL",
                String.format(
                        "jdbc:postgresql://%s:%s/patientdb",
                        patientDb.getDatabaseInstance().getDbInstanceEndpointAddress(),
                        patientDb.getDatabaseInstance().getDbInstanceEndpointPort()));
        patientEnv.put("SPRING_DATASOURCE_USERNAME", "admin_user");
        patientEnv.put("SPRING_DATASOURCE_PASSWORD", dbPassword);
        patientEnv.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
        patientEnv.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrapServers);
        patientEnv.put("BILLING_SERVICE_ADDRESS", billingServiceAddress);
        patientEnv.put("BILLING_SERVICE_PORT", "9001");

        ServiceConstruct patientService =
                new ServiceConstruct(
                        this,
                        "PatientService",
                        ecsCluster.getCluster(),
                        "patient-service",
                        List.of(4000),
                        patientEnv);
        patientService.getNode().addDependency(patientDb.getDatabaseInstance());
        patientService.getNode().addDependency(billingService.getFargateService());
        if (mskCluster != null) {
            patientService.getNode().addDependency(mskCluster.getMskCluster());
        }

        // Step 6: API Gateway (Public-facing via ALB)
        Map<String, String> gatewayEnv = new HashMap<>();
        gatewayEnv.put("AUTH_SERVICE_URL", authServiceUrl);
        gatewayEnv.put("PATIENT_SERVICE_URL", patientServiceUrl);

        LogGroup gatewayLogGroup =
                LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                        .logGroupName("/ecs/api-gateway")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_DAY)
                        .build();

        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "ApiGatewayService")
                        .cluster(ecsCluster.getCluster())
                        .serviceName("api-gateway")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .desiredCount(1)
                        // Give Spring Boot enough time to start up before health checking
                        .healthCheckGracePeriod(Duration.seconds(60))
                        .taskImageOptions(
                                ApplicationLoadBalancedTaskImageOptions.builder()
                                        .image(ContainerImage.fromRegistry("api-gateway"))
                                        .containerPort(4003) // The correct port we exposed earlier!
                                        .environment(gatewayEnv)
                                        .enableLogging(true)
                                        .logDriver(
                                                LogDriver.awsLogs(
                                                        AwsLogDriverProps.builder()
                                                                .logGroup(gatewayLogGroup)
                                                                .streamPrefix("api-gateway")
                                                                .build()))
                                        .build())
                        // Ensure ALB is internet-facing
                        .publicLoadBalancer(true)
                        .build();
    }

    private static boolean isLocalStackDeploy() {
        return "true".equalsIgnoreCase(System.getenv("LOCALSTACK_DEPLOY"));
    }
}
