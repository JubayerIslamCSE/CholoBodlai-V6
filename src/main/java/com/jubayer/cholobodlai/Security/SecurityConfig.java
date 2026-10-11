package com.jubayer.cholobodlai.Security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter f) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeJson(res, 401, "Unauthorized"))
                        .accessDeniedHandler((req, res, ex) -> writeJson(res, 403, "Forbidden")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain webChain(
            HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(a -> a

                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                        "/forgot-password",
                        "/resend-verification",
                        "/reset-password",
                        "/verify-email",
                                "/ping",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/shipper/**")
                        .hasAnyRole("SHIPPER", "ADMIN")

                        .requestMatchers("/owner/**")
                        .hasAnyRole("OWNER", "ADMIN")

                        .requestMatchers("/trucks/**")
                        .hasAnyRole("OWNER", "ADMIN")

                        .requestMatchers("/trips/new")
                        .hasAnyRole("SHIPPER", "ADMIN")

                        .requestMatchers("/trips/**")
                        .hasAnyRole("SHIPPER", "OWNER", "ADMIN")

                        .anyRequest().authenticated())

                .formLogin(f -> f
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())

                .logout(l -> l
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }


    private static void writeJson(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + msg + "\"}");
    }
}