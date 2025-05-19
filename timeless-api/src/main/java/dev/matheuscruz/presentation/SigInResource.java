package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import dev.matheuscruz.infra.security.BCryptAdapter;
import dev.matheuscruz.infra.security.Groups;
import io.quarkus.panache.common.Parameters;
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Set;

@Path("/api/sign-in")
public class SigInResource {

    UserRepository userRepository;
    AESAdapter aesAdapter;

    public SigInResource(
            UserRepository userRepository,
            AESAdapter aesAdapter
    ) {
        this.userRepository = userRepository;
        this.aesAdapter = aesAdapter;
    }

    @POST
    public Response signIn(SignInRequest req) {

        String email = tryEncryptEmail(req);

        User user = userRepository.find("email = :email", Parameters.with("email", email))
                .firstResultOptional()
                .orElseThrow(NotFoundException::new);

        Boolean checked = BCryptAdapter.checkPassword(req.password, user.getPassword());

        if (!checked) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String token = Jwt.upn(user.getId())
                .groups(Set.of(Groups.USER.groupName()))
                .expiresIn(Duration.ofHours(4))
                .sign();

        return Response.ok(new SignInResponse(token)).build();
    }

    private String tryEncryptEmail(SignInRequest req) {
        try {
            return this.aesAdapter.encrypt(req.email());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new InternalServerErrorException(e);
        }
    }


    public record SignInRequest(String email, String password) {
    }

    public record SignInResponse(String token) {}
}
