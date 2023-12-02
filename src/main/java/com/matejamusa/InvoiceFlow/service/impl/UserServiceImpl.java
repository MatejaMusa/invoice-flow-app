package com.matejamusa.InvoiceFlow.service.impl;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.dtomapper.UserDTOMapper;
import com.matejamusa.InvoiceFlow.model.User;
import com.matejamusa.InvoiceFlow.repository.UserRepository;
import com.matejamusa.InvoiceFlow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.matejamusa.InvoiceFlow.dtomapper.UserDTOMapper.fromUser;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository<User> userRepository;
    @Override
    public UserDTO createUser(User user) {
        return fromUser(userRepository.create(user));
    }

    public UserDTO getUserByEmail(String email) {
        return fromUser(userRepository.getUserByEmail(email));
    }

    @Override
    public void sendVerificationCode(UserDTO user) {
        userRepository.sendVerificationCode(user);
    }

    @Override
    public User getUser(String email) {
        return userRepository.getUserByEmail(email);
    }

    @Override
    public UserDTO verifyCode(String email, String code) {
        return fromUser(userRepository.verifyCode(email, code));
    }
}
