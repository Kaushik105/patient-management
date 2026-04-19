package org.pm.patientservice.repository;

import org.pm.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);
    boolean existsById(UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);

}
