package com.dotani.support;

import com.dotani.auth.service.PasswordResetMailer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/** Replaces SMTP with an in-memory outbox so tests can read the reset links that would be emailed. */
@TestConfiguration
public class TestMailConfig {

    public record SentLink(String to, String link) {
    }

    public static class RecordingPasswordResetMailer extends PasswordResetMailer {

        private final List<SentLink> outbox = new ArrayList<>();

        RecordingPasswordResetMailer(ObjectProvider<JavaMailSender> mailSender) {
            super(mailSender, "test@dotani.test", false, "");
        }

        @Override
        public synchronized void send(String to, String resetLink, long validMinutes) {
            outbox.add(new SentLink(to, resetLink));
        }

        public synchronized List<SentLink> outbox() {
            return List.copyOf(outbox);
        }

        public synchronized void clear() {
            outbox.clear();
        }
    }

    @Bean
    @Primary
    RecordingPasswordResetMailer recordingPasswordResetMailer(ObjectProvider<JavaMailSender> mailSender) {
        return new RecordingPasswordResetMailer(mailSender);
    }
}
