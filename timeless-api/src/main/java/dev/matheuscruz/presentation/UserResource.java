package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/api/users")
@RequestScoped
public class UserResource {

    @Claim(standard = Claims.upn)
    String upn;

    final UserRepository userRepository;

    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PATCH
    @Transactional
    public Response update(PatchUserRequest req) {

        if (!upn.equals(req.id())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User user = this.userRepository.find("id = :id", Parameters.with("id", upn)).firstResultOptional()
                .orElseThrow(NotFoundException::new);

        user.addPhoneNumber(req.phoneNumber());

        this.userRepository.update("phoneNumber = :phoneNumber where id = :id",
                Parameters.with("phoneNumber", req.phoneNumber()).and("id", upn));

        return Response.ok(user).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserInfo(@PathParam("id") String userId) {

        User user = this.userRepository.find("id = :id", Parameters.with("id", userId)).firstResultOptional()
                .orElseThrow(ForbiddenException::new);

        return Response.ok(new UserInfoResponse(user.getId(), user.getEmail(), user.getPhoneNumber(),
                user.getFirstName(), user.getLastName(), user.getPhoneNumber() != null)).build();
    }

    public record PatchUserRequest(String phoneNumber, String id) {
    }

    public record UserInfoResponse(String id, String email, String phoneNumber, String firstName, String lastName,
            Boolean hasPhoneNumber) {
    }
}
