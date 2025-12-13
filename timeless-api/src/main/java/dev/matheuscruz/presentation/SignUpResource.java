package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import dev.matheuscruz.infra.security.BCryptAdapter;
import dev.matheuscruz.presentation.data.Problem;
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

        boolean exists = this.userRepository.existsByEmail(req.email());
        if (exists) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new Problem("Este nome de usuário já foi usado. Tente outro.")).build();
        }

        User user = User.create(req.email(), BCryptAdapter.encrypt(req.password()), req.firstName(), req.lastName());

        QuarkusTransaction.requiringNew().run(() -> {
            this.userRepository.persist(user);
        });

        return Response.status(Response.Status.CREATED).entity(new SignUpResponse(user.getId(), user.getEmail()))
                .build();
    }

    public record SignUpRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 32) String password,
            @NotBlank String firstName, @NotBlank String lastName) {
    }

    public record SignUpResponse(String id, String email) {
    }
}
