package dev.matheuscruz.domain;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;
    @Column(unique = true)
    private String email;
    private String password;
    private String name;
    @Column(unique = true, name = "phone_number")
    private String phoneNumber;

    public static User create(String email, String password, String name, String phoneNumber) {
        return new User(
                Objects.requireNonNull(email),
                Objects.requireNonNull(password),
                Objects.requireNonNull(name),
                Objects.requireNonNull(phoneNumber)
        );
    }

    protected User() {
    }

    private User(String email, String password, String name, String phoneNumber) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.password = password;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
