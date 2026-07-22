package server.sassedo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import server.sassedo.security.jwt.AuthEntryPointJwt;
import server.sassedo.security.jwt.AuthTokenFilter;
import server.sassedo.user.service.user.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/lost-password/**").permitAll()
                        .requestMatchers("/api/common/helper-texts").permitAll()
                        .requestMatchers("/api/notification/fcm-token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/countries").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/countries/*/cities").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/countries/cities/popular").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/countries/cities/\\d+/image")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("/api/.*/admin.*")).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/faq/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hero-carousel").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/hero-carousel/images/\\d+")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blog").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/blog/images/\\d+")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/blog/[^/]+")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/roommate-listings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/roommate-listings/random").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/roommate-listings/photos/*").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/roommate-listings/\\d+")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/roommate-listings/\\d+/picture")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rental-listings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rental-listings/random").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rental-listings/photos/*").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/rental-listings/\\d+")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/rental-listings/\\d+/picture")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/promotion-packages").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook/**").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("/api/.*/moderator.*")).hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers("/api/questions/ask").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("/api/cards/\\d+/picture")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("/api/user/\\d+/picture")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("/api/deeplink/.*")).permitAll()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
