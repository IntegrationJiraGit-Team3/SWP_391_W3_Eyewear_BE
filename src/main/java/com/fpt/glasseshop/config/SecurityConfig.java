package com.fpt.glasseshop.config;

import com.fpt.glasseshop.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // ✅ Stateless JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers("/", "/login/**", "/register/**").permitAll()
                        .requestMatchers("/api/users/login").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(
                                "/api/users/login",
                                "/api/users/create",
                                "/api/users/check-email",
                                "/api/users/login-google"

                        ).permitAll()


                        // ===== PUBLIC PRODUCTS =====
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // ===== ADMIN =====
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN") // 🔥 FIX

                        // ===== STAFF =====
                        .requestMatchers(HttpMethod.GET, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").permitAll()

                        // ===== USER =====
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/checkout").authenticated()
                        .requestMatchers("/api/orders/my").authenticated()
                        .requestMatchers("/api/orders/user/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").authenticated() // 🔥 FIX
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated() // 🔥 FIX
                        .requestMatchers("/api/user-prescriptions").authenticated() // 🔥 FIX
                        .requestMatchers("/api/user-prescriptions/**").authenticated() // 🔥 FIX


                        // ===== RETURN REQUEST =====
                        .requestMatchers(HttpMethod.POST, "/api/return-requests/**").authenticated()

                        .requestMatchers("/operational-staff/**").hasAnyRole("OPERATIONAL_STAFF", "ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/return-requests/**")
                        .hasAnyRole("ADMIN", "OPERATIONAL_STAFF")
                        .anyRequest().authenticated()
                )

                // ❌ JWT → disable form login
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        // ✅ JWT FILTER
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}