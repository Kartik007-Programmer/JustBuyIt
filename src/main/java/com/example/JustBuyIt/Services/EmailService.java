package com.example.JustBuyIt.Services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private final Resend resend;
    private final String baseUrl;
    private final String fromEmail;

    private static final String VERIFICATION_TEMPLATE_ID = "email-verification-template-1";
    private static final String PASSWORD_RESET_TEMPLATE_ID = "reset-password-template-1";

    public EmailService(
            @Value("${RESEND_API_KEY}") String resendApiKey,
            @Value("${app.base-url}") String baseUrl,
            @Value("${MAIL_FROM:onboarding@resend.dev}") String fromEmail) {
        this.resend = new Resend(resendApiKey);
        this.baseUrl = baseUrl;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationEmail(String to, String token, String name) {
        String link = baseUrl + "/auth/v1/verify-email?token=" + token;

        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", name);
        variables.put("VERIFICATION_URL", link);

        send(to, VERIFICATION_TEMPLATE_ID, variables);
    }

    public void sendPasswordResetEmail(String to, String token, String name) {
        String link = baseUrl + "/ResetPassword.html?token=" + token;

        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", name);
        variables.put("RESET_URL", link);

        send(to, PASSWORD_RESET_TEMPLATE_ID, variables);
    }

    private void send(String to, String templateId, Map<String, Object> variables) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .template(Template.builder()
                        .id(templateId)
                        .variables(variables)
                        .build())
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            System.err.println("Resend send failed: " + e.getMessage());
        }
    }
}