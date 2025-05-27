package dev.matheuscruz.infra.persistence;

import dev.matheuscruz.domain.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public boolean existsByEmail(String email) {
        return count("email = :email", Parameters.with("email", email)) > 0;
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", phoneNumber)).firstResultOptional();
    }
}
