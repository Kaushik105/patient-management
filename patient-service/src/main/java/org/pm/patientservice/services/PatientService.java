package org.pm.patientservice.services;

import org.pm.patientservice.dto.PatientRequestDto;
import org.pm.patientservice.exception.EmailAlreadyExistsException;
import org.pm.patientservice.grpc.BillingServiceGrpcClient;
import org.pm.patientservice.kafka.KafkaProducer;
import org.pm.patientservice.mapper.PatientMapper;
import org.pm.patientservice.model.Patient;
import org.pm.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PatientService {


    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(
            PatientRepository patientRepository,
            BillingServiceGrpcClient billingServiceGrpcClient,
            KafkaProducer kafkaProducer){
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.patientRepository = patientRepository;
        this.kafkaProducer = kafkaProducer;
    }

    // get all patients
    public List<Patient> getPatients(){
        return patientRepository.findAll();
    }

    // get a patient by id
    public Patient getPatientById(UUID id) {
        return patientRepository.findById(id).orElse(null);
    }

    // create a patient
    public Patient createPatient(PatientRequestDto patientRequestDto) {
        if(patientRepository.existsByEmail(patientRequestDto.getEmail())){
            throw new EmailAlreadyExistsException("Patient with email " + patientRequestDto.getEmail() + " already exists.");
        }

        Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDto));

        billingServiceGrpcClient.CreateBillingAccount(patient.getId().toString(), patient.getName(), patient.getEmail());

        kafkaProducer.sendEvent(patient);

        return patient;
    }

    // update a patient
    public Patient updatePatient(UUID id, PatientRequestDto patientRequestDto) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patientRepository.existsByEmailAndIdNot(patientRequestDto.getEmail(), id))  {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        if (patient != null) {
            // Update patient details
            Patient patientDetails = PatientMapper.toModel(patientRequestDto);

            patient.setName(patientDetails.getName());
            patient.setEmail(patientDetails.getEmail());
            patient.setAddress(patientDetails.getAddress());
            patient.setDateOfBirth(patientDetails.getDateOfBirth());
            patient.setDateOfRegister(patientDetails.getDateOfRegister());
            return patientRepository.save(patient);
        }
        return null;
    }

    // delete a patient
    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }

}
