package org.pm.patientservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.pm.patientservice.dto.PatientRequestDto;
import org.pm.patientservice.dto.PatientResponseDto;
import org.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import org.pm.patientservice.mapper.PatientMapper;
import org.pm.patientservice.model.Patient;
import org.pm.patientservice.services.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient", description = "Patient endpoints")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // get all patients
    @Operation(description = "Get patients")
    @GetMapping("")
    public ResponseEntity<List<PatientResponseDto>> getPatients(){
        return ResponseEntity.ok(patientService.getPatients().stream().map(PatientMapper::toDto).collect(Collectors.toList()));
    }

    // get a patient by id
    @Operation(description = "Get a patient by Id")
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDto> getPatientById(@PathVariable UUID id) {
        Patient patient = patientService.getPatientById(id);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(PatientMapper.toDto(patient));
    }


    // create a patient
    @Operation(description = "Create a patient")
    @PostMapping("")
    public ResponseEntity<PatientResponseDto> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDto patientRequestDto) {
        Patient createdPatient = patientService.createPatient(patientRequestDto);
        return ResponseEntity.ok().body(PatientMapper.toDto(createdPatient));
    }

    // update a patient
    @Operation(description = "Update a patient")
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDto> updatePatient(@PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequestDto patientRequestDto) {
        Patient updatedPatient = patientService.updatePatient(id, patientRequestDto);
        return ResponseEntity.ok(PatientMapper.toDto(updatedPatient));
    }

    // delete a patient
    @Operation(description = "Delete a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }


}
