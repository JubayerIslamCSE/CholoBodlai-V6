package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.DTO.AuthResponse;
import com.jubayer.cholobodlai.DTO.LoginRequest;
import com.jubayer.cholobodlai.DTO.RegisterRequest;
import com.jubayer.cholobodlai.DTO.RegistrationResponse;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Security.AppUserDetails;
import com.jubayer.cholobodlai.Security.JwtService;
import com.jubayer.cholobodlai.Service.AccountService;
import com.jubayer.cholobodlai.Service.AccountRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final AccountRecoveryService recovery;
    private final AccountService accountService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authManager, AccountService accountService,
                          JwtService jwtService, UserDetailsService userDetailsService, AccountRecoveryService recovery) {
        this.recovery = recovery;
        this.authManager = authManager;
        this.accountService = accountService;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest r) {


        User user = accountService.register(
                r.fullName(),
                r.phone(),
                r.email(),
                r.password(),
                r.role(),
                r.truckRegistrationNo(),
                r.truckTypeId()
        );


        try {
            recovery.sendVerification(user);
        } catch (IllegalStateException unavailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new RegistrationResponse(user.getPhone(), user.getRole().name(),
                            user.effectiveApprovalStatus().name(),
                            "Account created, but email delivery is unavailable. Use Resend Verification later."));
        }
        boolean pending = user.getRole() == Role.OWNER;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(
                        user.getPhone(),
                        user.getRole().name(),
                        user.effectiveApprovalStatus().name(),
                        pending
                                ? "Email verification and admin approval required"
                                : "Account created. Please verify your email before logging in."
                ));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest r) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(r.phone(), r.password()));
        } catch (BadCredentialsException | DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone or password");
        }
        AppUserDetails ud = (AppUserDetails) userDetailsService.loadUserByUsername(r.phone());
        if (!ud.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Account is pending or disabled");
        }
        String token = jwtService.generateToken(ud);
        return ResponseEntity.ok(new AuthResponse(token, ud.getUsername(), ud.getFullName(), ud.getRole().name()));
    }
}