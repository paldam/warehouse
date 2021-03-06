package com.damian.boundry.rest;

import com.damian.domain.user.User;
import com.damian.domain.user.UserRepository;
import com.damian.security.SecurityUtils;
import com.damian.security.jwt.JWTConfigurer;
import com.damian.security.jwt.TokenProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;

import static com.damian.config.Constants.ANSI_RESET;
import static com.damian.config.Constants.ANSI_YELLOW;

@CrossOrigin
@RestController
public class UserJWTController {

    private final Logger log = LoggerFactory.getLogger(UserJWTController.class);
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private UserRepository userRepository;

    public UserJWTController(TokenProvider tokenProvider, AuthenticationManager authenticationManager,
                             UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }


    @PostMapping("/auth")
    public ResponseEntity authorize(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProvider.createToken(authentication, rememberMe);
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
            if (!SecurityUtils.isCurrentUserInRole("punkty")) {
                return ResponseEntity.ok(new JWTToken(jwt));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Brak uprawnień");
            }
        } catch (AuthenticationException ae) {
            log.trace("Authentication exception trace: {}", ae);
            return new ResponseEntity<>(Collections.singletonMap("AuthenticationException", ae.getLocalizedMessage())
                , HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/auth_loyalty_program")
    public ResponseEntity authorizeLoyaltyUser(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {
        if (loginVM.getUsername().contains("@")) {
            Optional<User> user = userRepository.findOneByEmail(loginVM.getUsername());
            user.ifPresent(user1 -> loginVM.setUsername(user1.getLogin()));
        }
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProvider.createToken(authentication, rememberMe);
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
            System.out.println(ANSI_YELLOW + SecurityUtils.getCurrentUserLogin() + ANSI_RESET);
            if (SecurityUtils.isCurrentUserInRole("punkty")) {
                return ResponseEntity.ok(new JWTToken(jwt));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Brak uprawnień");
            }
        } catch (AuthenticationException ae) {
            log.trace("Authentication exception trace: {}", ae);
            return new ResponseEntity<>(Collections.singletonMap("AuthenticationException", ae.getLocalizedMessage())
                , HttpStatus.UNAUTHORIZED);
        }
    }

    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
