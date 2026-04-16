package org.kaushik.services;

import org.kaushik.dto.PatientRequestDto;
import org.kaushik.exception.EmailAlreadyExistsException;
import org.kaushik.mapper.PatientMapper;
import org.kaushik.model.Patient;
import org.kaushik.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PatientService {


    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
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
        return patientRepository.save(PatientMapper.toModel(patientRequestDto));
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
