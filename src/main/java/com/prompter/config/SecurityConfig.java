package com.prompter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // Public: landing page, login, static assets, health.
                // /index.html must be permitted too: PageController serves it via
                // forward:/index.html and the FORWARD dispatch is re-checked here.
                .requestMatchers("/", "/index.html", "/login", "/login.html", "/css/**", "/js/**", "/img/**", "/actuator/**").permitAll()
                // Public API convention: endpoints under /api/public/**
                .requestMatchers("/api/public/**").permitAll()
                // Backoffice pages — always require login
                .requestMatchers("/backoffice/**").authenticated()
                // Everything else (other API endpoints, pages) requires login
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/backoffice", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        return http.build();
    }
}
