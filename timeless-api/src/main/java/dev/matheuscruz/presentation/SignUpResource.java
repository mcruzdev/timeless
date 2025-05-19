package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import dev.matheuscruz.infra.security.BCryptAdapter;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Path("/api/sign-up")
public class SignUpResource {

    AESAdapter aesAdapter;
    UserRepository userRepository;

    public SignUpResource(
            UserRepository userRepository,
            AESAdapter aesAdapter
    ) {
        this.userRepository = userRepository;
        this.aesAdapter = aesAdapter;
    }

    @POST
    public Response signUp(@Valid SignUpRequest req) {
        User user = tryGenerateUser(req);

        QuarkusTransaction.requiringNew().run(() -> {
            this.userRepository.persist(user);
        });

        return Response.created(URI.create("/api/sign-in")).build();
    }

    private User tryGenerateUser(SignUpRequest req) {
        try {
            String password = BCryptAdapter.encrypt(req.password());
            String email = this.aesAdapter.encrypt(req.email());
            String phoneNumber = this.aesAdapter.encrypt(req.phoneNumber());
            String name = this.aesAdapter.encrypt(req.name());
            return User.create(email, password, name, phoneNumber);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new InternalServerErrorException(e);
        }
    }

    public record SignUpRequest(
            @Email
            String email,
            @NotBlank
            @Size(min = 8)
            String password,
            @NotBlank
            String phoneNumber,
            @NotBlank
            String name) {
    }
}