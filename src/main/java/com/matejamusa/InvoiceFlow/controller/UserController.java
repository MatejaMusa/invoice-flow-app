package com.matejamusa.InvoiceFlow.controller;

import com.matejamusa.InvoiceFlow.enumeration.EventType;
import com.matejamusa.InvoiceFlow.event.NewUserEvent;
import com.matejamusa.InvoiceFlow.exception.ApiException;
import com.matejamusa.InvoiceFlow.form.*;
import com.matejamusa.InvoiceFlow.model.HttpResponse;
import com.matejamusa.InvoiceFlow.model.User;
import com.matejamusa.InvoiceFlow.dto.UserDTO;
import com.matejamusa.InvoiceFlow.model.UserPrincipal;
import com.matejamusa.InvoiceFlow.provider.TokenProvider;
import com.matejamusa.InvoiceFlow.service.EventService;
import com.matejamusa.InvoiceFlow.service.RoleService;
import com.matejamusa.InvoiceFlow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.matejamusa.InvoiceFlow.dtomapper.UserDTOMapper.toUser;
import static com.matejamusa.InvoiceFlow.utils.ExceptionUtils.processError;
import static com.matejamusa.InvoiceFlow.utils.UserUtils.getAuthenticatedUser;
import static com.matejamusa.InvoiceFlow.utils.UserUtils.getLoggedInUser;
import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated;

@RestController
@RequestMapping(path="/user")
@RequiredArgsConstructor
public class UserController {
    private static final String TOKEN_PREFIX = "Bearer ";
    private final UserService userService;
    private final RoleService roleService;
    private final EventService eventService;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ApplicationEventPublisher publisher;

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(@RequestBody @Valid LoginForm loginForm) {
        UserDTO user = authenticate(loginForm.getEmail(), loginForm.getPassword());
        return user.isUsingMfa() ? sendVerification(user) : sendResponse(user);
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(@RequestBody @Valid User user) {
        UserDTO userDTO = userService.createUser(user);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDTO))
                        .message(String.format("Account created for user %s", user.getFirstName()))
                        .status(HttpStatus.CREATED)
                        .statusCode(HttpStatus.CREATED.value())
                        .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> profile(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(getAuthenticatedUser(authentication).getEmail());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", user, "events", eventService.getEventsByUserId(user.getId()), "roles", roleService.getRoles()))
                        .message("Profile Retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }
    @PatchMapping("/update")
    public ResponseEntity<HttpResponse> updateUser(@RequestBody @Valid UpdateForm user) {
        UserDTO updatedUser = userService.updatedUserDetails(user);
        publisher.publishEvent(new NewUserEvent(updatedUser.getEmail(), EventType.PROFILE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", updatedUser, "events", eventService.getEventsByUserId(updatedUser.getId()), "roles", roleService.getRoles()))
                        .message("User updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/resetpassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) {
        userService.resetPassword(email);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Email sent. Please check your email to reset password.")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/verify/code/{email}/{code}")
    public ResponseEntity<HttpResponse> verifyCode(@PathVariable("email") String email, @PathVariable("code") String code) {
        UserDTO user = userService.verifyCode(email, code);
        publisher.publishEvent(new NewUserEvent(user.getEmail(), EventType.LOGIN_ATTEMPT_SUCCESS));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", user, "access_token",tokenProvider.createAccessToken(getUserPrincipal(user)),
                                "refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }
    @GetMapping("/verify/account/{key}")
    public ResponseEntity<HttpResponse> verifyAccount(@PathVariable("key") String key) {
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message(userService.verifyAccountKey(key).isEnabled() ? "Account already verified" : "Account Verified")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/verify/password/{key}")
    public ResponseEntity<HttpResponse> verifyPasswordUrl(@PathVariable("key") String key) {
        UserDTO user = userService.verifyPasswordKey(key);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", user))
                        .message("Please enter a new password.")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/refresh/token")
    public ResponseEntity<HttpResponse> refreshToken(HttpServletRequest request) {
        if(isHeaderAndTokenValid(request)) {
            String token = request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length());
            UserDTO user = userService.getUserById(tokenProvider.getSubject(token,request));
            return ResponseEntity.ok().body(
                    HttpResponse.builder()
                            .timeStamp(now().toString())
                            .data(Map.of("user", user, "access_token",tokenProvider.createAccessToken(getUserPrincipal(user)),
                                    "refresh_token", token))
                            .message("Token refreshed")
                            .status(OK)
                            .statusCode(OK.value())
                            .build());
        }
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason("Refresh Token missing or invalid")
                        .developerMessage("Refresh Token missing or invalid")
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build());
    }

    private boolean isHeaderAndTokenValid(HttpServletRequest request) {
        return request.getHeader(AUTHORIZATION) != null
                && request.getHeader(AUTHORIZATION).startsWith(TOKEN_PREFIX)
                && tokenProvider.isTokenValid(
                        tokenProvider.getSubject(request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length()),request),
                        request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length())
                );
    }

    @PutMapping("/new/password")
    public ResponseEntity<HttpResponse> resetPasswordWithKey(@RequestBody @Valid NewPasswordForm form) {
        userService.updatePassword(form.getUserId(), form.getPassword(), form.getConfirmPassword());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Password reset successfully.")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/password")
    public ResponseEntity<HttpResponse> updatePassword(Authentication authentication, @RequestBody @Valid UpdatePasswordForm form) {
        UserDTO userDTO = getAuthenticatedUser(authentication);
        userService.updatePassword(userDTO.getId(), form.getCurrentPassword(), form.getNewPassword(), form.getConfirmNewPassword());
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), EventType.PASSWORD_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserById(userDTO.getId()), "events", eventService.getEventsByUserId(userDTO.getId()), "roles", roleService.getRoles()))
                        .message("Password updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/role/{roleName}")
    public ResponseEntity<HttpResponse> updateUserRole(Authentication authentication, @PathVariable("roleName") String roleName) {
        UserDTO userDTO = getAuthenticatedUser(authentication);
        userService.updateUserRole(userDTO.getId(), roleName);
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), EventType.ROLE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userDTO.getId()), "events", eventService.getEventsByUserId(userDTO.getId()), "roles", roleService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Role updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/settings")
    public ResponseEntity<HttpResponse> updateAccountSettings(Authentication authentication, @RequestBody @Valid SettingsForm form) {
        UserDTO userDTO = getAuthenticatedUser(authentication);
        userService.updateAccountSettings(userDTO.getId(), form.getEnabled(), form.getNotLocked());
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), EventType.ACCOUNT_SETTINGS_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userDTO.getId()), "events", eventService.getEventsByUserId(userDTO.getId()), "roles", roleService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Account settings updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/togglemfa")
    public ResponseEntity<HttpResponse> toggleMfa(Authentication authentication) {
        UserDTO userDTO = userService.toggleMfa(getAuthenticatedUser(authentication).getEmail());
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), EventType.MFA_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userDTO, "events", eventService.getEventsByUserId(userDTO.getId()), "roles", roleService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Multi-Factor Authentication updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/image")
    public ResponseEntity<HttpResponse> updateProfileImage(Authentication authentication, @RequestParam("image") MultipartFile image) {
        UserDTO user = getAuthenticatedUser(authentication);
        userService.updateImage(user,image);
        publisher.publishEvent(new NewUserEvent(user.getEmail(), EventType.PROFILE_PICTURE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(user.getId()), "events", eventService.getEventsByUserId(user.getId()), "roles", roleService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Profile image updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping(value = "/image/{fileName}", produces = IMAGE_PNG_VALUE)
    public byte[] getProfileImage(@PathVariable("fileName") String fileName) throws Exception{
        return Files.readAllBytes(Paths.get(System.getProperty("user.home") + "/Downloads/images/" + fileName));
    }

    @RequestMapping("/error")
    public ResponseEntity<HttpResponse> handleError(HttpServletRequest httpServletRequest) {
        return ResponseEntity.badRequest().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason("There is no mapping for a " + httpServletRequest.getMethod() + " request for this path on the server")
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build());
    }

    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toString());
    }

    private ResponseEntity<HttpResponse> sendResponse(UserDTO user) {
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", user, "access_token",tokenProvider.createAccessToken(getUserPrincipal(user)),
                                "refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private UserPrincipal getUserPrincipal(UserDTO user) {
        return new UserPrincipal(toUser(userService.getUserByEmail(user.getEmail())), roleService.getRoleByUserId(user.getId()));
    }

    private ResponseEntity<HttpResponse> sendVerification(UserDTO user) {
        userService.sendVerificationCode(user);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", user))
                        .message("Verification Code Sent")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }
    private UserDTO authenticate(String email, String password) {
        try {
            if(userService.getUserByEmail(email) != null) {
                publisher.publishEvent(new NewUserEvent(email, EventType.LOGIN_ATTEMPT));
            }
            Authentication authentication = authenticationManager.authenticate(unauthenticated(email, password));
            UserDTO loggedInUser = getLoggedInUser(authentication);
            if(!loggedInUser.isUsingMfa()) {
                publisher.publishEvent(new NewUserEvent(email, EventType.LOGIN_ATTEMPT_SUCCESS));
            }
            return loggedInUser;
        } catch (Exception e) {
            publisher.publishEvent(new NewUserEvent(email, EventType.LOGIN_ATTEMPT_FAILURE));
            processError(request, response, e);
            throw new ApiException(e.getMessage());
        }

    }
}
