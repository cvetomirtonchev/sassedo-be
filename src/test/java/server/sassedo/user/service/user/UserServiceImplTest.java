package server.sassedo.user.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.repository.PasswordTokenRepository;
import server.sassedo.user.repository.RoleRepository;
import server.sassedo.user.repository.UserRepository;
import server.sassedo.user.service.EmailVerificationService;

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
