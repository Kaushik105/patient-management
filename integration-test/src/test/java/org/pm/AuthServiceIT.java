package org.pm;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthServiceIT {

    @BeforeAll
    public static void setup() {
        // Point RestAssured to Auth Service (assumes it is running on port 4005)
        RestAssured.baseURI = "http://localhost:4005";
    }

    @Test
    public void testUserRegistrationAndLogin() {
        // 1. Register User
        String registerPayload = """
                {
                    "password": "password123",
                    "email": "test@it.com",
                    "role": "user"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(registerPayload)
        .when()
            .post("/register")
        .then()
            .statusCode(200);

        // 2. Login User
        String loginPayload = """
                {
                    "email": "test@it.com",
                    "password": "password123"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(loginPayload)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }
}
