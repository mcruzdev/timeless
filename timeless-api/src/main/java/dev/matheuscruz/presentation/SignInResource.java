package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import dev.matheuscruz.infra.security.BCryptAdapter;
import dev.matheuscruz.infra.security.Groups;
import io.quarkus.panache.common.Parameters;
import io.smallrye.jwt.build.Jwt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Path("/api/sign-in")
public class SignInResource {

    UserRepository userRepository;
    AESAdapter aesAdapter;
    EntityManager entityManager;

    public SignInResource(UserRepository userRepository, AESAdapter aesAdapter, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.aesAdapter = aesAdapter;
        this.entityManager = entityManager;
    }

    @POST
    @Transactional
    public Response signIn(@Valid SignInRequest req) {

        Optional<User> userOptional = userRepository.find("email = :email", Parameters.with("email", req.email()))
                .firstResultOptional();

        User user = null;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {

            try {
                user = (User) entityManager.createNativeQuery("SELECT * FROM users WHERE email = :email", User.class)
                        .setParameter("email", req.email()).getSingleResult();
                
                Boolean checked = BCryptAdapter.checkPassword(req.password(), user.getPassword());

                if (!checked) {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
                
                String encryptedEmail = aesAdapter.encrypt(req.email());
                String encryptedPhone = user.getPhoneNumber() != null ? aesAdapter.encrypt(user.getPhoneNumber())
                        : null;

                entityManager.createNativeQuery("UPDATE users SET email = :email, phone_number = :phone WHERE id = :id")
                        .setParameter("email", encryptedEmail).setParameter("phone", encryptedPhone)
                        .setParameter("id", user.getId()).executeUpdate();
                
            } catch (NoResultException e) {
                throw new ForbiddenException();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Boolean checked = BCryptAdapter.checkPassword(req.password(), user.getPassword());

        if (!checked) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String token = Jwt.upn(user.getId()).groups(Set.of(Groups.USER.groupName())).expiresIn(Duration.ofDays(1))
                .sign();

        return Response.ok(new SignInResponse(token, user.getId(), user.fullName(), req.email(), user.hasPhoneNumber()))
                .build();
    }

    public record SignInRequest(@Email String email, @NotBlank @Size(min = 6) String password) {
    }

    public record SignInResponse(String token, String id, String name, String email, Boolean hasPhoneNumber) {
    }
}
