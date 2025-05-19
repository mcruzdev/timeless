package dev.matheuscruz.domain.notification;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class NotificationService {

    Mailer mailer;
    Boolean enabled;

    public NotificationService(Mailer mailer, @ConfigProperty(name = "timeless.email.enabled", defaultValue = "false") Boolean enabled) {
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
