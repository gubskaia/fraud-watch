package com.fraudwatch.gateway.config;

import com.fraudwatch.gateway.security.JwtAuthenticationFilter;
import com.fraudwatch.gateway.security.RestAccessDeniedHandler;
import com.fraudwatch.gateway.security.RestAuthenticationEntryPoint;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
        RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/internal/info",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAuthority("USER_SELF_READ")
                .requestMatchers(HttpMethod.POST, "/api/accounts", "/api/transactions").hasAuthority("TRANSACTION_CREATE")
                .requestMatchers(HttpMethod.GET, "/api/accounts/**", "/api/transactions/**").hasAuthority("TRANSACTION_READ")
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").hasAuthority("REVIEW_CASE_READ")
                .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasAuthority("REVIEW_CASE_DECIDE")
                .requestMatchers(HttpMethod.GET, "/api/fraud/rules", "/api/fraud/decisions/**").hasAuthority("FRAUD_RULE_READ")
                .requestMatchers(HttpMethod.PUT, "/api/fraud/rules/**").hasAuthority("FRAUD_RULE_WRITE")
                .requestMatchers(HttpMethod.GET, "/api/audit/**").hasAuthority("AUDIT_READ")
                .requestMatchers(HttpMethod.GET, "/api/notifications/**").hasAuthority("NOTIFICATION_READ")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
