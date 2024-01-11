package com.matejamusa.InvoiceFlow.repository.impl;

import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.enumeration.VerificationType;
import com.matejamusa.InvoiceFlow.exception.ApiException;
import com.matejamusa.InvoiceFlow.form.UpdateForm;
import com.matejamusa.InvoiceFlow.model.Role;
import com.matejamusa.InvoiceFlow.model.User;
import com.matejamusa.InvoiceFlow.model.UserPrincipal;
import com.matejamusa.InvoiceFlow.repository.RoleRepository;
import com.matejamusa.InvoiceFlow.rowmapper.UserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import com.matejamusa.InvoiceFlow.repository.UserRepository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.matejamusa.InvoiceFlow.enumeration.RoleType.ROLE_USER;
import static com.matejamusa.InvoiceFlow.enumeration.VerificationType.ACCOUNT;
import static com.matejamusa.InvoiceFlow.enumeration.VerificationType.PASSWORD;
import static com.matejamusa.InvoiceFlow.query.UserQuery.*;
import static com.matejamusa.InvoiceFlow.utils.SMSUtils.sendSMS;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository<User>, UserDetailsService {

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository<Role> roleRepository;
    private final BCryptPasswordEncoder encoder;
    private final SnsClient snsClient;

    @Override
    public User create(User user) {
        if(getEmailCount(user.getEmail().trim().toLowerCase()) > 0) throw new ApiException("Email is use, please use different email and try again.");
        try {
            KeyHolder holder = new GeneratedKeyHolder();
            SqlParameterSource parameters = getSqlParameterSource(user);
            jdbc.update(INSERT_USER_QUERY, parameters, holder);
            user.setId(requireNonNull(holder.getKey()).longValue());
            roleRepository.addRoleToUser(user.getId(), ROLE_USER.name());
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
            jdbc.update(INSERT_ACCOUNT_VERIFICATION_URL_QUERY, Map.of("userId",user.getId(), "url", verificationUrl));
//            emailService.sendVerificationUrl(user.getFirstName(),user.getEmail(), verificationUrl, ACCOUNT);
            user.setEnabled(false);
            user.setNotLocked(true);
            return user;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Collection<User> list(int page, int pageSize) {
        return null;
    }

    @Override
    public User get(Long id) {
        try {
            return jdbc.queryForObject(SELECT_USER_BY_ID_QUERY, Map.of("id", id), new UserRowMapper());
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException("No User found by id: " + id);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User update(User data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    private Integer getEmailCount(String email) {
        return jdbc.queryForObject(COUNT_USER_EMAIL_QUERY, Map.of("email", email), Integer.class);
    }

    private SqlParameterSource getSqlParameterSource(User user) {
        return new MapSqlParameterSource()
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("password",encoder.encode(user.getPassword()));
    }

    private SqlParameterSource getUserDetailsSqlParameterSource(UpdateForm user) {
        return new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("phone", user.getPhone())
                .addValue("address", user.getAddress())
                .addValue("title", user.getTitle())
                .addValue("bio", user.getBio());
    }

    private String getVerificationUrl(String key, String type) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/verify/" + type + "/" + key).toUriString();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = getUserByEmail(email);
        if(user == null) {
            log.error("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        } else {
            log.info("User found in the database: {}", email);
            UserPrincipal up = new UserPrincipal(user, roleRepository.getRoleByUserId(user.getId()));
            return up;
        }
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, Map.of("email",email), new UserRowMapper());
            return user;
        }catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("No user found by email: "+ email);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void sendVerificationCode(UserDTO user) {
        String expirationDate = format(addDays(new Date(),1),DATE_FORMAT);
        String verificationCode = randomAlphabetic(8).toUpperCase();
        try {
            jdbc.update(DELETE_VERIFICATION_CODE_BY_USER_ID_QUERY, Map.of("id",user.getId()));
            jdbc.update(INSERT_VERIFICATION_CODE_QUERY, Map.of("userId",user.getId(), "code", verificationCode, "expirationDate", expirationDate));
//            sendSMS(snsClient, "From: InvoiceFlow \nVerification code\n"+ verificationCode, user.getPhone());
            log.info("Verification code: {}", verificationCode);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyCode(String email, String code) {
        if(isVerificationCodeExpired(code)) throw new ApiException("This code has expired, please log in again");
        try {
            User userByCode = jdbc.queryForObject(SELECT_USER_BY_USER_CODE_QUERY, Map.of("code", code), new UserRowMapper());
            User userByEmail = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, Map.of("email", email), new UserRowMapper());
            if(userByCode.getEmail().equalsIgnoreCase(userByEmail.getEmail())) {
                jdbc.update(DELETE_CODE_QUERY, Map.of("code", code));
                return userByCode;
            } else {
                throw new ApiException("Code is invalid. Please try again.");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException("Could not find record");
        } catch (Exception e) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void resetPassword(String email) {
        if(getEmailCount(email.trim().toLowerCase()) <= 0) {
            throw new ApiException("There is no account with this email address.");
        }
        try {
           String expirationDate = format(addDays(new Date(), 1), DATE_FORMAT);
           User user = getUserByEmail(email);
           String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), PASSWORD.getType());
           jdbc.update(DELETE_PASSWORD_VERIFICATION_BY_USER_ID_QUERY, Map.of("userId", user.getId()));
           jdbc.update(INSERT_PASSWORD_VERIFICATION_QUERY, Map.of("userId", user.getId(), "url", verificationUrl, "expirationDate", expirationDate));
           log.info("Verification URL: {}", verificationUrl);
        } catch (Exception e) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyPasswordKey(String key) {
        if(isLinkExpired(key,PASSWORD)) throw new ApiException("This link has expired, please reset password again.");
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_PASSWORD_URL_QUERY, Map.of("url", getVerificationUrl(key,PASSWORD.getType())), new UserRowMapper());
            return user;
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException("This link is not valid. Please reset your password again");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void renewPassword(String key, String password, String confirmPassword) {
        if(!password.equals(confirmPassword)) throw new ApiException("Passwords do not match");
        try {
            jdbc.update(UPDATE_USER_PASSWORD_BY_URL_QUERY, Map.of("password", encoder.encode(password), "url", getVerificationUrl(key,PASSWORD.getType())));
            jdbc.update(DELETE_VERIFICATION_BY_URL_QUERY, Map.of("url", getVerificationUrl(key,PASSWORD.getType())));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyAccountKey(String key) {
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_ACCOUNT_URL_QUERY, Map.of("url",getVerificationUrl(key,ACCOUNT.getType())), new UserRowMapper());
            jdbc.update(UPDATE_USER_ENABLED_QUERY, Map.of("enabled",true, "id",user.getId()));
            return user;
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException("This link is not valid.");
        } catch (Exception e) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User updateUserDetails(UpdateForm user) {
        try {
            jdbc.update(UPDATE_USER_DETAILS_QUERY, getUserDetailsSqlParameterSource(user));
            return get(user.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException("No User found by id: " + user.getId());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("This link is not valid. Please reset your password again");
        }
    }

    @Override
    public void updatePassword(Long id, String currentPassword, String newPassword, String confirmNewPassword) {
        if(!newPassword.equals(confirmNewPassword)) {
            throw new ApiException("Passwords don't match. Please try again.");
        }
        User user = get(id);
        if(!encoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException("Incorrect current password. Please try again.");
        }
        try {
            jdbc.update(UPDATE_USER_PASSWORD_BY_ID_QUERY, Map.of("userId", id, "password", encoder.encode(newPassword)));
        } catch (Exception e) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked) {
        try {
            jdbc.update(UPDATE_USER_SETTINGS_QUERY, Map.of("userId", userId, "enabled", enabled, "notLocked", notLocked));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User toggleMfa(String email) {
        User user = getUserByEmail(email);
        if(isBlank(user.getPhone())) {
            throw new ApiException("You need phone number to change Multi-Factor Authentication.");
        }
        user.setUsingMfa(!user.isUsingMfa());
        try {
            jdbc.update(TOGGLE_USER_MFA_QUERY, Map.of("email", email, "isUsingMfa", user.isUsingMfa()));
            return user;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("Unable to update Multi-Factor Authentication.");
        }
    }

    private Boolean isLinkExpired(String key, VerificationType password) {
        try {
            return jdbc.queryForObject(SELECT_EXPIRATION_BY_URL_QUERY, Map.of("url", getVerificationUrl(key,password.getType())), Boolean.class);
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException("This link is not valid. Please reset your password again");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    private Boolean isVerificationCodeExpired(String code) {
        try {
            return jdbc.queryForObject(SELECT_CODE_EXPIRATION_QUERY, Map.of("code", code), Boolean.class);
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException("This code is not valid, please log in again");
        } catch (Exception e) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }
}
