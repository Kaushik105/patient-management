package org.pm.analyticsservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.event.PatientEvent;

@Service
public class KafkaConsumerClient {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerClient.class);

    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void receiveEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            log.info(
                    "Received event for patientId: {}, eventType: {}",
                    patientEvent.getPatientId(),
                    patientEvent.getEventType());
        } catch (Exception e) {
            log.error("Error processing Kafka patient event", e);
        }
    }
}
