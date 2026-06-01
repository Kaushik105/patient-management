package org.pm;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ApiGatewayIT {

    private static String validToken;

    @BeforeAll
    public static void setup() {
        // Gateway port
        RestAssured.baseURI = "http://localhost:4003";

        // Register and login to get valid token for test
        String registerPayload = """
                {
                    "email": "gateway@it.com",
                    "password": "password123",
                    "role": "user"
                }
                """;

        // Try register via Gateway. If exist, fail ok, login work anyway.
        given()
            .contentType(ContentType.JSON)
            .body(registerPayload)
        .when()
            .post("/api/auth/register");

        String loginPayload = """
                {
                    "email": "gateway@it.com",
                    "password": "password123"
                }
                """;

        validToken = given()
            .contentType(ContentType.JSON)
            .body(loginPayload)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract()
            .path("token");
    }

    @Test
    public void testPatientServiceRoute_WithoutToken_Returns401() {
        given()
        .when()
            .get("/api/patients")
        .then()
            .statusCode(401);
    }

    @Test
    public void testPatientServiceRoute_WithInvalidToken_Returns401() {
        given()
            .header("Authorization", "Bearer invalid.token.here")
        .when()
            .get("/api/patients")
        .then()
            .statusCode(401);
    }

    @Test
    public void testPatientServiceRoute_WithValidToken_Returns200() {
        // Assume patient-service running and /api/patients valid endpoint
        given()
            .header("Authorization", "Bearer " + validToken)
        .when()
            .get("/api/patients")
        .then()
            .statusCode(200);
    }
}
