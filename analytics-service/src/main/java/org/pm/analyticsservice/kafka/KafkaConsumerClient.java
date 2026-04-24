package org.pm.analyticsservice.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.event.PatientEvent;

@Service
public class KafkaConsumerClient {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaConsumerClient(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void receiveEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // Process the event (e.g., log it, update analytics, etc.)
            System.out.println("Received event for patientId: " + patientEvent.getPatientId() + ", eventType: " + patientEvent.getEventType());
        } catch (Exception e) {
            System.err.println("Error processing event: " + e.getMessage());
        }
    }
}
