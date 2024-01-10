package com.matejamusa.InvoiceFlow.service;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.form.UpdateForm;
import com.matejamusa.InvoiceFlow.model.User;

public interface UserService {
    UserDTO createUser(User user);

    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);

    UserDTO verifyCode(String email, String code);

    void resetPassword(String email);

    UserDTO verifyPasswordKey(String key);

    void renewPassword(String key, String password, String confirmPassword);

    UserDTO verifyAccountKey(String key);

    UserDTO updatedUserDetails(UpdateForm user);

    UserDTO getUserById(Long userId);

    void updatePassword(Long userId, String currentPassword, String newPassword, String confirmNewPassword);

    void updateUserRole(Long userId, String roleName);

    void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked);
}
