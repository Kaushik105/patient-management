package org.pm.patientservice.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub billingServiceBlockingStub;
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String billingServiceAddress,
            @Value("${billing.service.port:9001}") int billingServicePort
    ) {
        log.info("Initializing BillingServiceGrpcClient with address: {} and port: {}", billingServiceAddress, billingServicePort);
        ManagedChannel channel = io.grpc.ManagedChannelBuilder.forAddress(billingServiceAddress, billingServicePort)
                .usePlaintext()
                .build();
        billingServiceBlockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse CreateBillingAccount(String  patientId, String name, String email) {
        log.info("Creating billing account for patientId: {}, name: {}, email: {}", patientId, name, email);
        billing.BillingRequest request = billing.BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();
        BillingResponse response = billingServiceBlockingStub.createBillingAccount(request);
        log.info("Received billing account creation response: {}", response);
        return response;
    }
}
