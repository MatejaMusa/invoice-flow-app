package com.matejamusa.InvoiceFlow.service.impl;

import com.matejamusa.InvoiceFlow.enumeration.VerificationType;
import com.matejamusa.InvoiceFlow.exception.ApiException;
import com.matejamusa.InvoiceFlow.service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    @Override
    public void sendVerificationEmail(String firstName, String email, String verificationUrl, VerificationType verificationType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("matejamusa6@gmail.com");
            message.setTo(email);
            message.setText(getEmailMessage(firstName, verificationUrl, verificationType));
            message.setSubject(String.format("InvoiceFlow - %s Verification Email", StringUtils.capitalize(verificationType.getType())));
            mailSender.send(message);
            log.info("Email sent to {}", firstName);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String getEmailMessage(String firstName, String verificationUrl, VerificationType verificationType) {
        switch (verificationType) {
            case PASSWORD -> { return "Hello " + firstName + "\n\nReset password request. Please click link below to reset your password. \n\n" + verificationUrl + "\n\nThe Support Team";}
            case ACCOUNT -> { return "Hello " + firstName + "\n\nYour account has been created. Please click link below to verify your account. \n\n" + verificationUrl + "\n\nThe Support Team";}
            default -> throw new ApiException("Unable to send email. Email type unknown.");
        }
    }
}
