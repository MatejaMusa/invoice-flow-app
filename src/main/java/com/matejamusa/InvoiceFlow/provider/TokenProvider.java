package com.matejamusa.InvoiceFlow.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.matejamusa.InvoiceFlow.model.UserPrincipal;
import com.matejamusa.InvoiceFlow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class TokenProvider {
    private static final String INVOICE_FLOW_DOO = "INVOICE_FLOW_DOO";
    private static final String CUSTOMER_MANAGEMENT_SERVICE = "CUSTOMER_MANAGEMENT_SERVICE";
    private static final String AUTHORITIES = "authorities";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 3_000_000;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 432_000_000;
    @Value("${jwt.secret}")
    private String secret;
    private final UserService userService;

    public String createAccessToken(UserPrincipal userPrincipal) {
        String[] claims = getClaimsFromUser(userPrincipal);
        return JWT.create().withIssuer(INVOICE_FLOW_DOO).withAudience(CUSTOMER_MANAGEMENT_SERVICE)
                .withIssuedAt(new Date()).withSubject(String.valueOf(userPrincipal.getUser().getId())).withArrayClaim(AUTHORITIES, claims)
                .withExpiresAt(new Date(currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    public String createRefreshToken(UserPrincipal userPrincipal) {
        String[] claims = getClaimsFromUser(userPrincipal);
        return JWT.create().withIssuer(INVOICE_FLOW_DOO).withAudience(CUSTOMER_MANAGEMENT_SERVICE)
                .withIssuedAt(new Date()).withSubject(String.valueOf(userPrincipal.getUser().getId()))
                .withExpiresAt(new Date(currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    public Long getSubject(String token, HttpServletRequest request) {
        try {
            return Long.valueOf(getJWTVerifier().verify(token).getSubject());
        } catch (TokenExpiredException e) {
            request.setAttribute("expiredMessage", e.getMessage());
            throw e;
        } catch (InvalidClaimException e) {
            request.setAttribute("invalidClaim", e.getMessage());
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    public List<GrantedAuthority> getAuthorities(String token) {
        String[] claims = getClaimsFromToken(token);
        return stream(claims).map(SimpleGrantedAuthority::new).collect(toList());
    }

    public Authentication getAuthentication(Long userId, List<GrantedAuthority> authorities, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken userPasswordAuthToken = new UsernamePasswordAuthenticationToken(userService.getUserById(userId), null, authorities);
        userPasswordAuthToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return userPasswordAuthToken;
    }

    public boolean isTokenValid(Long userId, String token) {
        JWTVerifier verifier = getJWTVerifier();
        return !Objects.isNull(userId) && !isTokenExpired(verifier, token);
    }

    private boolean isTokenExpired(JWTVerifier verifier, String token) {
        Date expiration = verifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }

    private String[] getClaimsFromUser(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new);
    }
    private String[] getClaimsFromToken(String token) {
        JWTVerifier verifier = getJWTVerifier();
        return verifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    private JWTVerifier getJWTVerifier() {
        JWTVerifier verifier;
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            verifier = JWT.require(algorithm).withIssuer(INVOICE_FLOW_DOO).build();
        }catch (JWTVerificationException e) {throw new JWTVerificationException("Token cannot be verified");}
        return verifier;
    }
}
