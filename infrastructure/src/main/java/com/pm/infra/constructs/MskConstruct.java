package com.pm.infra.constructs;

import java.util.stream.Collectors;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.constructs.Construct;

/**
 * MskConstruct encapsulates the Amazon Managed Streaming for Apache Kafka (MSK) cluster.
 *
 * <p>It provisions a Kafka cluster within the isolated subnets of the VPC, ensuring that the
 * message broker is highly secure and not accessible from the public internet.
 */
public class MskConstruct extends Construct {

    private final CfnCluster mskCluster;

    public MskConstruct(Construct scope, String id, Vpc vpc) {
        super(scope, id);

        this.mskCluster =
                CfnCluster.Builder.create(this, id + "KafkaCluster")
                        .clusterName("patient-management-kafka")
                        // LocalStack's MSK provider rejects older 2.x versions.
                        .kafkaVersion("3.6.0")
                        // LocalStack validates broker count against the two isolated subnet AZs.
                        .numberOfBrokerNodes(2)
                        .brokerNodeGroupInfo(
                                CfnCluster.BrokerNodeGroupInfoProperty.builder()
                                        // Smallest instance type for Kafka to minimize costs in dev
                                        .instanceType("kafka.t3.small")
                                        // We place the brokers in the isolated subnets we created
                                        // earlier.
                                        .clientSubnets(
                                                vpc.getIsolatedSubnets().stream()
                                                        .map(ISubnet::getSubnetId)
                                                        .collect(Collectors.toList()))
                                        // Standard distribution strategy
                                        .brokerAzDistribution("DEFAULT")
                                        .build())
                        .build();
    }

    public CfnCluster getMskCluster() {
        return mskCluster;
    }
}
