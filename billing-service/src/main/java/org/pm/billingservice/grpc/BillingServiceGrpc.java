package org.pm.billingservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingServiceGrpc extends BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpc.class);

    @Override
    public void createBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {

        log.info("Received billing request for {}", request.toString());

        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("ACC-6493574198")
                .setStatus("ACTIVE")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
