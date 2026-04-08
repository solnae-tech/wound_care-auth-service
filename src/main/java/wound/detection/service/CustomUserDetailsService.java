package wound.detection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wound.detection.model.User;
import wound.detection.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                // OAuth users have no password — use empty string to avoid NPE
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                user.getIsActive(),          // enabled
                true,                         // accountNonExpired
                true,                         // credentialsNonExpired
                true,                         // accountNonLocked
                Collections.emptyList()       // no roles/authorities needed for this service
        );
    }
}
