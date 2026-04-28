package com.superme.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS config must be FIRST
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v1/v3/api-docs/**",
                                "/v1/v3/api-docs.yaml",
                                "/v1/faq",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/v1/ws/**"   // 👈 allow WS handshake
                        ).permitAll()

                        // Allow all OPTIONS requests for CORS preflight (FIRST RULE)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Static resources and error pages
                        .requestMatchers(
                                "/v1/ws/**",
                                "/error",
                                "/favicon.ico",
                                "/actuator/health")
                        .permitAll()


                        // Public User Auth endpoints
                        .requestMatchers(
                                "/v1/auth/register",
                                "/v1/auth/check-referral",
                                "/v1/auth/register/generate-otp",
                                "/v1/auth/register/verify-otp",
                                "/v1/auth/login",
                                "/v1/auth/forgot-password/**",
                                "/v1/auth/verify-email",
                                "/v1/auth/add-family-member",
                                "/v1/auth/leaderboard/streaks",
                                "/v1/auth/users-by-role",
                                "/v1/auth/users-logged-in-today",
                                "/v1/ads",
                                "/v1/auth/check/**")
                        .permitAll()

                        // Public Admin Auth endpoints
                        .requestMatchers(
                                "/v1/admin/auth/login",
                                "/v1/admin/auth/register")
                        .permitAll()

                        // Protected Admin Auth endpoints (require ADMIN role)
                        .requestMatchers(
                                "/v1/admin/auth/update",
                                "/v1/admin/auth/delete",
                                "/admin/api/s3/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Admin User Management endpoints (require ADMIN role)
                        .requestMatchers(
                                "/v1/admin/users/**",
                                "/v1/admin/dashboard/**",
                                "/v1/admin/reports/**",
                                "/v1/admin/analytics/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Public Family endpoints
                        .requestMatchers(
                                "/v1/family/create",
                                "/v1/family/join",
                                "/v1/family/add-member",
                                "/v1/family/check-family-code",
                                "/v1/family/switch/**")
                        .permitAll()

                        // Tasks endpoints - Allow all for now to fix CORS
                        .requestMatchers("/v1/tasks/**").permitAll()

                        // Authenticated User Auth endpoints (require USER or ADMIN role)
                        .requestMatchers(
                                "/v1/auth/logout",
                                "/v1/auth/delete-account",
                                "/v1/auth/members",
                                "/v1/auth/user-details/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Authenticated Family endpoints (require USER or ADMIN role)
                        .requestMatchers(
                                "/v1/family/members",
                                "/v1/family/code",
                                "/v1/family/generate-join-token",
                                "/v1/family/leave",
                                "/v1/family/remove-all",
                                "/v1/family/remove")
                        .hasAnyRole("USER", "ADMIN")

                        // User-specific endpoints (require USER or ADMIN role)
                        .requestMatchers(
                                "/v1/habits/**",
                                "/v1/notes/**",
                                "/v1/journal/**",
                                "/v1/calendar/**",
                                "/v1/profile/**",
                                "/v1/badges/**",
                                "/v1/challenges/**",
                                "/learning",
                                "/v1/coins/**",
                                "/v1/settings/**",
                                "/v1/reels/**",
                                "/v1/trophies/**",
                                "/v1/tutors/**",
                                "/v1/tasks/**",
                                "/v1/courses/**",
                                "/v1/articles/**",
                                "/v1/category",
                                "/v1/mood/vibes/**",
                                "/v1/api/s3/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Catch-all for any remaining admin endpoints
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Use either setAllowedOriginPatterns OR setAllowedOrigins, not both.
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // For dev/testing
        // Remove setAllowedOrigins if using setAllowedOriginPatterns!
        // configuration.setAllowedOrigins(Arrays.asList(
        // "http://localhost:4200",
        // "http://127.0.0.1:4200",
        // "http://localhost:3000"));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v1/ws",
                "/v1/ws/**"
        );
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
