package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.security.BCryptAdapter;
import dev.matheuscruz.infra.security.Groups;
import io.quarkus.panache.common.Parameters;
import io.smallrye.jwt.build.Jwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Set;

@Path("/api/sign-in")
public class SignInResource {

    UserRepository userRepository;

    public SignInResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    public Response signIn(@Valid SignInRequest req) {

        User user = userRepository.find("email = :email", Parameters.with("email", req.email())).firstResultOptional()
                .orElseThrow(ForbiddenException::new);

        Boolean checked = BCryptAdapter.checkPassword(req.password(), user.getPassword());

        if (!checked) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String token = Jwt.upn(user.getId()).groups(Set.of(Groups.USER.groupName())).expiresIn(Duration.ofDays(1))
                .sign();

        return Response.ok(new SignInResponse(token, user.getId(), user.fullName(), req.email(), user.hasPhoneNumber()))
                .build();
    }

    public record SignInRequest(@Email String email, @NotBlank @Size(min = 8, max = 32) String password) {
    }

    public record SignInResponse(String token, String id, String name, String email, Boolean hasPhoneNumber) {
    }
}
