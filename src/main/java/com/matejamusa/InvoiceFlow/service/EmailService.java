package com.matejamusa.InvoiceFlow.service;

import com.matejamusa.InvoiceFlow.enumeration.VerificationType;

public interface EmailService {
    void sendVerificationEmail(String firstName, String email, String verificationUrl, VerificationType verificationType);
}
