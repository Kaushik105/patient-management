package com.pm.infra.constructs;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

/**
 * EcsClusterConstruct encapsulates the Amazon Elastic Container Service (ECS) cluster.
 *
 * <p>It provides the compute environment for our Docker containers (microservices). LocalStack
 * deployment keeps this cluster simple and avoids Cloud Map because the current local license does
 * not support service discovery.
 */
public class EcsClusterConstruct extends Construct {

    private final Cluster cluster;

    public EcsClusterConstruct(Construct scope, String id, Vpc vpc) {
        super(scope, id);

        this.cluster =
                Cluster.Builder.create(this, id + "Cluster")
                        .clusterName("patient-management-cluster")
                        .vpc(vpc)
                        .build();
    }

    public Cluster getCluster() {
        return cluster;
    }
}
