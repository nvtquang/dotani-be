package com.dotani.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends password reset links over SMTP (configure SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME,
 * SPRING_MAIL_PASSWORD). Without SMTP the link is written to the log in development only.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean logLinksWhenUnconfigured;
    private final boolean smtpConfigured;

    public PasswordResetMailer(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:no-reply@dotani.local}") String from,
            @Value("${app.mail.log-links-when-unconfigured:true}") boolean logLinksWhenUnconfigured,
            // docker-compose passes an empty SPRING_MAIL_HOST when SMTP is not set up
            @Value("${spring.mail.host:}") String smtpHost
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.logLinksWhenUnconfigured = logLinksWhenUnconfigured;
        this.smtpConfigured = smtpHost != null && !smtpHost.isBlank();
    }

    public void send(String to, String resetLink, long validMinutes) {
        JavaMailSender sender = smtpConfigured ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            if (logLinksWhenUnconfigured) {
                log.info("SMTP is not configured; password reset link for {}: {}", to, resetLink);
            } else {
                log.warn("SMTP is not configured; could not send a password reset email to {}", to);
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("DOTANI - Đặt lại mật khẩu");
        message.setText("""
                Xin chào,

                Bạn (hoặc ai đó) vừa yêu cầu đặt lại mật khẩu tài khoản DOTANI.
                Mở liên kết sau để đặt mật khẩu mới (hết hạn sau %d phút, chỉ dùng được một lần):

                %s

                Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu hiện tại vẫn giữ nguyên.
                """.formatted(validMinutes, resetLink));
        try {
            sender.send(message);
        } catch (MailException exception) {
            // the request still succeeds: the user can ask again, and the endpoint never reveals account existence
            log.error("Could not send password reset email to {}", to, exception);
        }
    }
}
