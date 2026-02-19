package dev.matheuscruz.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InboundMessageRepository implements PanacheRepositoryBase<InboundMessage, String> {
}
