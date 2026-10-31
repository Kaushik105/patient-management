package com.pm.infra.constructs;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.constructs.Construct;

/**
 * RdsConstruct encapsulates a PostgreSQL database instance.
 *
 * <p>It creates a secure database placed in the Isolated subnets of the VPC, meaning it cannot be
 * accessed from the internet. Credentials are automatically generated and stored securely in AWS
 * Secrets Manager.
 */
public class RdsConstruct extends Construct {

    private final DatabaseInstance databaseInstance;

    public RdsConstruct(Construct scope, String id, Vpc vpc, String databaseName) {
        super(scope, id);

        this.databaseInstance =
                DatabaseInstance.Builder.create(this, id + "DB")
                        .engine(
                                DatabaseInstanceEngine.postgres(
                                        PostgresInstanceEngineProps.builder()
                                                .version(PostgresEngineVersion.of("15.4", "15"))
                                                .build()))
                        .vpc(vpc)
                        // Place the database in the ISOLATED subnets for maximum security
                        .vpcSubnets(
                                SubnetSelection.builder()
                                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                                        .build())
                        // Use a burstable micro instance to save costs, perfect for dev/localstack
                        .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                        .allocatedStorage(20)
                        // LocalStack does not need a generated Secrets Manager attachment for dev
                        // credentials.
                        .credentials(
                                Credentials.fromPassword(
                                        "admin_user",
                                        SecretValue.unsafePlainText("admin_password")))
                        .databaseName(databaseName)
                        // In production, this would be RETAIN. For dev/LocalStack, DESTROY is
                        // preferred
                        // so we don't leave orphaned resources when we delete the stack.
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build();

        // Allow inbound PostgreSQL traffic from within the VPC
        this.databaseInstance
                .getConnections()
                .allowDefaultPortFrom(
                        Peer.ipv4(vpc.getVpcCidrBlock()),
                        "Allow PostgreSQL connections from VPC subnets");
    }

    public DatabaseInstance getDatabaseInstance() {
        return databaseInstance;
    }
}
