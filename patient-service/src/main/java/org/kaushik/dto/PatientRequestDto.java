package org.kaushik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.kaushik.dto.validators.CreatePatientValidationGroup;

public class PatientRequestDto {
    @NotBlank(message="name is required")
    @Size(max=100, message = "name length can't exceed 100")
    private String name;

    @NotBlank(message = "email can't be blank")
    @Email(message = "email is not valid")
    private String email;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "date of birth is required")
    private String dateOfBirth;

    @NotBlank( groups=CreatePatientValidationGroup.class,message = "date of register is required")
    private String dateOfRegister;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfRegister() {
        return dateOfRegister;
    }

    public void setDateOfRegister(String dateOfRegister) {
        this.dateOfRegister = dateOfRegister;
    }
}
