package org.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private static final long DEFAULT_DEADLINE_SECONDS = 5;

    private final ManagedChannel channel;
    private final BillingServiceGrpc.BillingServiceBlockingStub billingServiceBlockingStub;

    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String billingServiceAddress,
            @Value("${billing.service.port:9001}") int billingServicePort) {
        log.info(
                "Initializing BillingServiceGrpcClient connecting to {}:{}",
                billingServiceAddress,
                billingServicePort);
        this.channel =
                ManagedChannelBuilder.forAddress(billingServiceAddress, billingServicePort)
                        .usePlaintext()
                        .build();
        this.billingServiceBlockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        log.info(
                "Creating billing account via gRPC for patientId: {}, name: {}, email: {}",
                patientId,
                name,
                email);
        BillingRequest request =
                BillingRequest.newBuilder()
                        .setPatientId(patientId)
                        .setName(name)
                        .setEmail(email)
                        .build();

        try {
            BillingResponse response =
                    billingServiceBlockingStub
                            .withDeadlineAfter(DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)
                            .createBillingAccount(request);
            log.info(
                    "Received billing response successfully: accountId={}, status={}",
                    response.getAccountId(),
                    response.getStatus());
            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "gRPC call to Billing Service failed. Status: {}, Description: {}",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription(),
                    e);
            throw e;
        }
    }

    /** Backward-compatible alias for createBillingAccount. */
    public BillingResponse CreateBillingAccount(String patientId, String name, String email) {
        return createBillingAccount(patientId, name, email);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down BillingServiceGrpcClient ManagedChannel...");
            channel.shutdown();
            try {
                if (!channel.awaitTermination(DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)) {
                    log.warn(
                            "ManagedChannel did not terminate within timeout; forcing"
                                    + " shutdownNow().");
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error(
                        "Interrupted while waiting for ManagedChannel to terminate; forcing"
                                + " shutdownNow().",
                        e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
