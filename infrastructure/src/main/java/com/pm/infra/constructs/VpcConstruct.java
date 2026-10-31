package com.pm.infra.constructs;

import java.util.List;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

/**
 * VpcConstruct encapsulates the network foundation of our architecture.
 *
 * <p>It provisions an Amazon Virtual Private Cloud (VPC) with: - maxAzs: 2 (High availability
 * across two Availability Zones) - Public Subnets: For the Application Load Balancer to receive
 * internet traffic. - Private Subnets (with NAT): For our ECS microservices. They can reach the
 * internet (e.g., to pull Docker images from public repositories) but cannot be reached FROM the
 * internet directly. - Isolated Subnets: For databases (RDS) and messaging (MSK). No internet route
 * at all.
 */
public class VpcConstruct extends Construct {

    private final Vpc vpc;

    public VpcConstruct(Construct scope, String id) {
        super(scope, id);

        this.vpc =
                Vpc.Builder.create(this, "PatientManagementVPC")
                        .vpcName("PatientManagementVPC")
                        // Use LocalStack-valid AZ names instead of CDK's unresolved dummy AZ
                        // placeholders.
                        .availabilityZones(List.of("us-east-1a", "us-east-1b"))
                        .natGateways(
                                1) // 1 NAT Gateway to save costs (in production, we might use 1 per
                        // AZ).
                        .subnetConfiguration(
                                List.of(
                                        SubnetConfiguration.builder()
                                                .name("Public")
                                                .subnetType(SubnetType.PUBLIC)
                                                .cidrMask(24) // 256 IPs per subnet
                                                .build(),
                                        SubnetConfiguration.builder()
                                                .name("Private")
                                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                                .cidrMask(24)
                                                .build(),
                                        SubnetConfiguration.builder()
                                                .name("Isolated")
                                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                                .cidrMask(24)
                                                .build()))
                        .build();
    }

    public Vpc getVpc() {
        return vpc;
    }
}
