package com.matejamusa.InvoiceFlow.service;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.model.User;

public interface UserService {
    UserDTO createUser(User user);

    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);

    User getUser(String email);

    UserDTO verifyCode(String email, String code);
}
