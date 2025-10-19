package com.blogapp.configurations;

import com.blogapp.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.security.config.Customizer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final CustomUserDetailsService customUserDetailsService;
  private final BCryptPasswordEncoder passwordEncoder;

  public SecurityConfig(
      CustomUserDetailsService customUserDetailsService, BCryptPasswordEncoder passwordEncoder) {
    this.customUserDetailsService = customUserDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  @Bean
  @Primary
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(customUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
    MvcRequestMatcher apiMatcher = new MvcRequestMatcher(introspector, "/api/**");
    http.authenticationProvider(authenticationProvider())
        .csrf(csrf -> csrf.ignoringRequestMatchers(apiMatcher))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").permitAll()
            .requestMatchers("/", "/login", "/register", "/css/**", "/js/**").permitAll()
            // Protect specific GET pages that should require authentication
            .requestMatchers(HttpMethod.GET, "/posts/new").authenticated()
            .requestMatchers(HttpMethod.GET, "/posts/*/edit").authenticated()
            .requestMatchers(HttpMethod.GET, "/posts/*/comments/*/edit").authenticated()
            .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/posts/*/comments").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .defaultAuthenticationEntryPointFor(
                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                apiMatcher)
            .defaultAccessDeniedHandlerFor(
                (request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN),
                apiMatcher))
        .httpBasic(Customizer.withDefaults())
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/loginabc")
            .defaultSuccessUrl("/", true)
            .failureUrl("/login?error=true")
            .permitAll())
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"));
    return http.build();
  }
}
