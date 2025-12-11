package dev.matheuscruz.domain;

import dev.matheuscruz.infra.persistence.converters.SecretAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(unique = true)
    @Convert(converter = SecretAttributeConverter.class)
    private String email;

    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Convert(converter = SecretAttributeConverter.class)
    @Column(unique = true, name = "phone_number")
    private String phoneNumber;

    public static User create(String email, String password, String firstName, String lastName, String phoneNumber) {
        return new User(Objects.requireNonNull(email), Objects.requireNonNull(password),
                Objects.requireNonNull(firstName), Objects.requireNonNull(lastName),
                Objects.requireNonNull(phoneNumber));
    }

    protected User() {
    }

    private User(String email, String password, String firstName, String lastName, String phoneNumber) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String fullName() {
        return "%s %s".formatted(firstName, lastName);
    }

    public void addPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
