package com.pm.infra.constructs;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

/**
 * ServiceConstruct encapsulates a standard internal microservice running on AWS Fargate.
 *
 * <p>It combines several moving parts into a single, clean interface: 1. Task Definition: How much
 * CPU/RAM does the app need? 2. Log Group: Sends all application logs to CloudWatch. 3. Container
 * Definition: Links the Docker image, ports, and environment variables. 4. Fargate Service: Ensures
 * the container is always running and registers it with Cloud Map (e.g., auth-service.pm.local).
 */
public class ServiceConstruct extends Construct {

    private final FargateService fargateService;

    public ServiceConstruct(
            Construct scope,
            String id,
            Cluster cluster,
            String imageName,
            List<Integer> ports,
            Map<String, String> envVars) {
        super(scope, id);

        // 1. Define the computing resources (Task Definition)
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, id + "TaskDef")
                        .cpu(256) // 0.25 vCPU - small and cheap for dev
                        .memoryLimitMiB(512) // 512 MB RAM
                        .build();

        // 2. Configure CloudWatch Logs
        LogGroup logGroup =
                LogGroup.Builder.create(this, id + "LogGroup")
                        .logGroupName("/ecs/" + imageName)
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_DAY)
                        .build();

        // 3. Define the Container
        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(
                                ContainerImage.fromRegistry(
                                        imageName)) // Image name matches our ECR repo/LocalStack
                        // image
                        .environment(envVars)
                        .logging(
                                LogDriver.awsLogs(
                                        AwsLogDriverProps.builder()
                                                .logGroup(logGroup)
                                                .streamPrefix(imageName)
                                                .build()));

        // Add all required ports to the container (e.g., HTTP and gRPC for billing)
        List<PortMapping> portMappings =
                ports.stream()
                        .map(
                                port ->
                                        PortMapping.builder()
                                                .containerPort(port)
                                                .hostPort(port)
                                                .protocol(Protocol.TCP)
                                                .build())
                        .toList();
        containerOptions.portMappings(portMappings);

        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        // 4. Create the Fargate Service
        this.fargateService =
                FargateService.Builder.create(this, id + "Service")
                        .cluster(cluster)
                        .taskDefinition(taskDefinition)
                        .assignPublicIp(false) // Ensure it stays purely in the private subnet
                        .serviceName(imageName) // This defines its Cloud Map DNS name (e.g.
                        // "patient-service.pm.local")
                        .build();
    }

    public FargateService getFargateService() {
        return fargateService;
    }
}
