package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.presentation.data.UserInfoResponse;
import io.quarkus.panache.common.Parameters;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/users")
public class UserResource {

    final UserRepository userRepository;

    public UserResource(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @PATCH
    @Transactional
    public Response update(PatchUserRequest req) {

        User user = this.userRepository.find("id = :id", Parameters.with("id", req.id()))
                .firstResultOptional()
                .orElseThrow(ForbiddenException::new);

        user.addPhoneNumber(req.phoneNumber());

        this.userRepository.persist(user);

        return Response.ok(user).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserInfo(@PathParam("id") String userId) {

        User user = this.userRepository.find("id = :id", Parameters.with("id", userId))
                .firstResultOptional()
                .orElseThrow(ForbiddenException::new);

        return Response.ok(new UserInfoResponse(user.getId(), user.getEmail(), user.getPhoneNumber(), user.getFirstName(), user.getLastName(), user.getPhoneNumber() != null)).build();
    }

    public record PatchUserRequest(String phoneNumber, String id) {
    }
}
