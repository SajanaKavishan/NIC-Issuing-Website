package com.project.nic.service;

import com.project.nic.model.AssistanceRequest;
import com.project.nic.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final HttpClient httpClient;
    private final boolean emailEnabled;
    private final boolean smsEnabled;
    private final String fromEmail;
    private final String smsGatewayUrl;
    private final String smsApiToken;

    public NotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notifications.email.enabled:false}") boolean emailEnabled,
            @Value("${app.notifications.sms.enabled:false}") boolean smsEnabled,
            @Value("${app.notifications.email.from:${spring.mail.username:no-reply@nic.local}}") String fromEmail,
            @Value("${app.notifications.sms.gateway-url:}") String smsGatewayUrl,
            @Value("${app.notifications.sms.api-token:}") String smsApiToken
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.fromEmail = fromEmail;
        this.smsGatewayUrl = smsGatewayUrl;
        this.smsApiToken = smsApiToken;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void applicationSubmitted(String type, Long id, String email, String phone) {
        String reference = reference(type, id);
        String message = "Your " + type + " application has been submitted. Reference: " + reference + ".";
        notifyCitizen(email, phone, "NIC application submitted", message);
    }

    public void applicationStatusChanged(String type, Long id, String status, String email, String phone) {
        String reference = reference(type, id);
        String message = "Your " + type + " application " + reference + " status is now " + label(status) + ".";
        notifyCitizen(email, phone, "NIC application status updated", message);
    }

    public void paymentRecorded(Payment payment) {
        String message = "Your payment " + payment.getPaymentId() + " for " + label(payment.getServiceType())
                + " NIC is " + label(payment.getStatus()) + ". Amount: " + payment.getAmount() + ".";
        notifyCitizen(payment.getEmail(), null, "NIC payment update", message);
    }

    public void assistanceReplySent(AssistanceRequest request) {
        String message = "A support reply has been added to your assistance request #" + request.getId() + ".";
        notifyCitizen(request.getEmail(), null, "NIC assistance reply", message);
    }

    private void notifyCitizen(String email, String phone, String subject, String message) {
        sendEmail(email, subject, message);
        sendSms(phone, message);
    }

    private void sendEmail(String to, String subject, String body) {
        if (isBlank(to)) {
            return;
        }
        if (!emailEnabled) {
            logger.info("Email notification skipped because email notifications are disabled. to='{}', subject='{}'", to, subject);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Email notification skipped because JavaMailSender is not available. to='{}'", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Email notification failed for '{}': {}", to, e.getMessage());
        }
    }

    private void sendSms(String to, String body) {
        if (isBlank(to)) {
            return;
        }
        if (!smsEnabled) {
            logger.info("SMS notification skipped because SMS notifications are disabled. to='{}'", to);
            return;
        }
        if (isBlank(smsGatewayUrl)) {
            logger.warn("SMS notification skipped because app.notifications.sms.gateway-url is not configured. to='{}'", to);
            return;
        }

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(smsGatewayUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"to\":\"" + json(to) + "\",\"message\":\"" + json(body) + "\"}"));
            if (!isBlank(smsApiToken)) {
                request.header("Authorization", "Bearer " + smsApiToken);
            }
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("SMS notification failed for '{}': gateway returned {}", to, response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("SMS notification failed for '{}': {}", to, e.getMessage());
        }
    }

    private String reference(String type, Long id) {
        String prefix = switch (type.toLowerCase()) {
            case "renew" -> "REN";
            case "lost" -> "LST";
            default -> "NEW";
        };
        return prefix + "-" + String.format("%05d", id == null ? 0 : id);
    }

    private String label(String value) {
        if (isBlank(value)) {
            return "pending";
        }
        return value.trim().replace('_', ' ').toLowerCase();
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
