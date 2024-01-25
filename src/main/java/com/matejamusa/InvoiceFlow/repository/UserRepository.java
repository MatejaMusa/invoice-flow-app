package com.matejamusa.InvoiceFlow.repository;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.form.UpdateForm;
import com.matejamusa.InvoiceFlow.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

public interface UserRepository <T extends User>{
    T create(T data);
    Collection<T> list(int page, int pageSize);
    T get(Long id);
    T update(T data);
    Boolean delete(Long id);

    User getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);

    User verifyCode(String email, String code);

    void resetPassword(String email);

    T verifyPasswordKey(String key);

    T verifyAccountKey(String key);

    T updateUserDetails(UpdateForm user);

    void updatePassword(Long id, String currentPassword, String newPassword, String confirmNewPassword);
    void updatePassword(Long userId, String password, String confirmPassword);

    void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked);

    User toggleMfa(String email);

    void updateImage(UserDTO user, MultipartFile image);
}
