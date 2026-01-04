package dev.matheuscruz.presentation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.presentation.data.Problem;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SignUpResourceTest {

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should return Created when valid SignUpRequest is provided")
    void should_returnCreated_when_validSignUpRequestIsProvided() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "password-1234", "John", "Doe");

        var response = given().contentType("application/json").body(request).when().post("/api/sign-up").then().log()
                .ifValidationFails().statusCode(201).extract().as(SignUpResource.SignUpResponse.class);

        assertThat(response.id()).isNotNull().isNotBlank();
        assertThat(response.email()).isEqualTo(request.email());
    }

    @Test
    @DisplayName("should return Conflict when email already exists")
    void should_returnConflict_when_emailAlreadyExists() {
        var existingEmail = "existing.user@email.com";
        var existingUser = User.create(existingEmail, "some-hashed-password", "Existing", "User");

        QuarkusTransaction.requiringNew().run(() -> {
            userRepository.persist(existingUser);
        });

        var request = new SignUpResource.SignUpRequest(existingEmail, "a-different-password", "New", "Person");

        var response = given().contentType("application/json").body(request).when().post("/api/sign-up").then().log()
                .ifValidationFails().statusCode(409).extract().as(Problem.class);

        assertThat(response.message()).isEqualTo("Este nome de usuário já foi usado. Tente outro.");
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.existsByEmail(existingEmail)).isTrue();
    }

    @Test
    @DisplayName("should return Bad Request when email is not valid")
    void should_returnBadRequest_when_emailIsNotValid() {
        var request = new SignUpResource.SignUpRequest("invalid-email.com", "password-1234", "John", "Doe");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactly(tuple("signUp.req.email", "must be a well-formed email address"));
    }

    @Test
    @DisplayName("should return Bad Request when password is too short")
    void should_returnBadRequest_when_passwordIsTooShort() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "1234", "John", "Doe");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactly(tuple("signUp.req.password", "size must be between 8 and 32"));
    }

    @Test
    @DisplayName("should return Bad Request when password is too long")
    void should_returnBadRequest_when_passwordIsTooLong() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "12345678910111213141516171819202122", "John",
                "Doe");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactly(tuple("signUp.req.password", "size must be between 8 and 32"));
    }

    @Test
    @DisplayName("should return Bad Request when password is blank")
    void should_returnBadRequest_when_passwordIsBlank() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "", "John", "Doe");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(2).extracting(Violation::field, Violation::message)
                .containsExactlyInAnyOrder(tuple("signUp.req.password", "size must be between 8 and 32"),
                        tuple("signUp.req.password", "must not be blank")

                );
    }

    @Test
    @DisplayName("should return Bad Request when first name is blank")
    void should_returnBadRequest_when_firstNameIsBlank() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "password-1234", "", "Doe");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactly(tuple("signUp.req.firstName", "must not be blank"));
    }

    @Test
    @DisplayName("should return Bad Request when last name is blank")
    void should_returnBadRequest_when_lastNameIsBlank() {
        var request = new SignUpResource.SignUpRequest("test@email.com", "password-1234", "John", "");

        var validationProblem = given().contentType("application/json").body(request).when().post("/api/sign-up").then()
                .log().ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(validationProblem.title()).isEqualTo("Constraint Violation");
        assertThat(validationProblem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactly(tuple("signUp.req.lastName", "must not be blank"));
    }

    @Test
    @DisplayName("should return Bad Request when e-mail is blank")
    void should_returnBadRequest_when_emailIsBlank() {
        var request = new SignUpResource.SignUpRequest("", "password-1234", "John", "Doe");

        var problem = given().contentType("application/json").body(request).when().post("/api/sign-up").then().log()
                .ifValidationFails().statusCode(400).extract().as(ValidationProblem.class);

        assertThat(problem.title()).isEqualTo("Constraint Violation");
        assertThat(problem.violations()).hasSize(1).extracting(Violation::field, Violation::message)
                .containsExactlyInAnyOrder(tuple("signUp.req.email", "must not be blank"));
    }

    public record Violation(String field, String message) {
    }

    public record ValidationProblem(String title, int status, java.util.List<Violation> violations) {
    }
}
