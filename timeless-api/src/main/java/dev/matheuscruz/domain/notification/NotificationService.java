package dev.matheuscruz.domain.notification;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationService {

    Mailer mailer;
    Boolean enabled;

    public NotificationService(Mailer mailer,
            @ConfigProperty(name = "timeless.email.enabled", defaultValue = "false") Boolean enabled) {
        this.mailer = mailer;
        this.enabled = enabled;
    }

    @Tool("send a email with output when necessary")
    public void sendEmail(String output) {
        if (enabled) {
            mailer.send(Mail.withText("dev@mailer.com", "Timeless notification", output));
        }
    }
}
