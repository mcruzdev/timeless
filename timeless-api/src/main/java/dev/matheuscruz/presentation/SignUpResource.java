package dev.matheuscruz.presentation;

import java.net.URI;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import dev.matheuscruz.infra.security.BCryptAdapter;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/sign-up")
public class SignUpResource {

    UserRepository userRepository;
    AESAdapter aesAdapter;

    public SignUpResource(UserRepository userRepository, AESAdapter aesAdapter) {
        this.userRepository = userRepository;
        this.aesAdapter = aesAdapter;
    }

    @POST
    public Response signUp(@Valid SignUpRequest req) {
        Log.info("congratulations! we are creating a new user");

        User user = User.create(req.email(), BCryptAdapter.encrypt(req.password()), req.firstName(), req.lastName());

        QuarkusTransaction.requiringNew().run(() -> {
            this.userRepository.persist(user);
        });

        return Response.created(URI.create("/api/sign-in")).build();
    }

    public record SignUpRequest(@Email String email, @NotBlank @Size(min = 8) String password,
            @NotBlank String firstName, @NotBlank String lastName) {
    }
}
