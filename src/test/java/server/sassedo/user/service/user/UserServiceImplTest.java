package server.sassedo.user.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.user.data.dto.ERole;
import server.sassedo.user.data.dto.Role;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.data.network.request.RegisterRequest;
import server.sassedo.user.repository.PasswordTokenRepository;
import server.sassedo.user.repository.RoleRepository;
import server.sassedo.user.repository.UserRepository;
import server.sassedo.user.service.EmailVerificationService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordTokenRepository passwordTokenRepository;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void registerUser_persistsWithoutACodeAndSendsRegistrationSuccessEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new-user@example.com");
        request.setPassword("secret123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhone("+359888123456");
        request.setAcceptedTerms(true);
        request.setAcceptedGdpr(true);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(roleRepository.findByName(ERole.ROLE_USER))
                .thenReturn(Optional.of(new Role(ERole.ROLE_USER)));

        service.registerUser(request, "http://localhost:8080");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getVerificationCode()).isNull();
        verify(emailVerificationService).sendRegistrationSuccess(
                request.getEmail(),
                "New User"
        );
    }

    @Test
    void searchUsers_detectsAnExactNumericIdAndCombinesItWithTheCityFilter() {
        Pageable pageable = PageRequest.of(0, 40);
        Page<User> expected = Page.empty(pageable);
        when(userRepository.searchUsers("42", 42L, 7L, pageable)).thenReturn(expected);

        Page<User> result = service.searchUsers(" 42 ", 7L, pageable);

        assertThat(result).isSameAs(expected);
        verify(userRepository).searchUsers("42", 42L, 7L, pageable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alice", "alice@example.com"})
    void searchUsers_keepsTextSearchWithoutTreatingItAsAnId(String search) {
        Pageable pageable = PageRequest.of(1, 40);
        Page<User> expected = Page.empty(pageable);
        when(userRepository.searchUsers(search, null, null, pageable)).thenReturn(expected);

        Page<User> result = service.searchUsers(search, null, pageable);

        assertThat(result).isSameAs(expected);
        verify(userRepository).searchUsers(search, null, null, pageable);
    }
}
