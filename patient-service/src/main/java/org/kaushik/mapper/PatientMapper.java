package org.kaushik.mapper;

import org.kaushik.dto.PatientRequestDto;
import org.kaushik.dto.PatientResponseDto;
import org.kaushik.model.Patient;

import java.time.LocalDate;

public class PatientMapper {
    public static PatientResponseDto toDto(Patient patient) {
        PatientResponseDto dto = new PatientResponseDto();
        dto.setId(patient.getId().toString());
        dto.setName(patient.getName());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setDateOfBirth(patient.getDateOfBirth().toString());
        return dto;
    }

    public static Patient toModel(PatientRequestDto patientRequestDto){
        Patient patient = new Patient();
        patient.setName(patientRequestDto.getName());
        patient.setEmail(patientRequestDto.getEmail());
        patient.setAddress(patientRequestDto.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDto.getDateOfBirth()));
        patient.setDateOfRegister(LocalDate.now());
        return patient;
    }
}
