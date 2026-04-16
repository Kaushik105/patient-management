package org.kaushik.controllers;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.kaushik.dto.PatientRequestDto;
import org.kaushik.dto.PatientResponseDto;
import org.kaushik.dto.validators.CreatePatientValidationGroup;
import org.kaushik.mapper.PatientMapper;
import org.kaushik.model.Patient;
import org.kaushik.services.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // get all patients
    @GetMapping("/patients")
    public ResponseEntity<List<PatientResponseDto>> getPatients(){
        return ResponseEntity.ok(patientService.getPatients().stream().map(PatientMapper::toDto).collect(Collectors.toList()));
    }

    // get a patient by id
    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientResponseDto> getPatientById(@PathVariable UUID id) {
        Patient patient = patientService.getPatientById(id);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(PatientMapper.toDto(patient));
    }


    // create a patient
    @PostMapping("/patients")
    public ResponseEntity<PatientResponseDto> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDto patientRequestDto) {
        Patient createdPatient = patientService.createPatient(patientRequestDto);
        return ResponseEntity.ok().body(PatientMapper.toDto(createdPatient));
    }

    // update a patient
    @PutMapping("/patients/{id}")
    public ResponseEntity<PatientResponseDto> updatePatient(@PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequestDto patientRequestDto) {
        Patient updatedPatient = patientService.updatePatient(id, patientRequestDto);
        return ResponseEntity.ok(PatientMapper.toDto(updatedPatient));
    }

    // delete a patient
    @DeleteMapping("/patients/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }


}
