package org.pm.patientservice.repository;

import java.util.UUID;
import org.pm.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);

    boolean existsById(UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
