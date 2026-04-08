package wound.detection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wound.detection.model.OAuthAccount;
import wound.detection.model.User;
import wound.detection.model.UserProfile;
import wound.detection.repository.OAuthAccountRepository;
import wound.detection.repository.UserProfileRepository;
import wound.detection.repository.UserRepository;
import wound.detection.security.OAuth2AuthenticatedPrincipal;

/**
 * Loads (or creates) a local {@link User} after Google successfully
 * authenticates.
 *
 * <p>This method is {@code @Transactional}, so the Hibernate session is open
 * for the entire execution. We extract all required plain values (id, email,
 * fullName) <em>before</em> the method returns so that the success handler
 * never needs to touch a lazy proxy after the session is closed.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository         userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserProfileRepository  userProfileRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1 — Fetch the raw Google payload (name, email, sub …)
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider       = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerUserId = oauth2User.getName();                                    // Google sub
        String email          = oauth2User.getAttribute("email");
        String name           = oauth2User.getAttribute("name");

        // 2 — Resolve or create the local User entity
        //     Everything below runs inside the same @Transactional session.
        User localUser = resolveLocalUser(email, name, provider, providerUserId);

        // 3 — Read the plain fields NOW while the session is still open.
        //     The success handler must never call getEmail() on the entity again
        //     because its Hibernate session will be gone by then.
        Long   userId   = localUser.getId();
        String dbEmail  = localUser.getEmail();
        String fullName = localUser.getFullName();

        // 4 — Wrap in our custom principal carrying plain values (not the entity)
        return new OAuth2AuthenticatedPrincipal(oauth2User, userId, dbEmail, fullName);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User resolveLocalUser(String email, String name,
                                  String provider, String providerUserId) {

        // Already linked via oauth_accounts → return existing user
        return oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .map(oa -> {
                    // Reload the user fully (not a lazy proxy) so getId() etc. are safe
                    return userRepository.findById(oa.getUser().getId())
                            .orElseThrow(() -> new RuntimeException("Linked user not found"));
                })
                .orElseGet(() -> createNewOAuthUser(email, name, provider, providerUserId));
    }

    private User createNewOAuthUser(String email, String name,
                                    String provider, String providerUserId) {

        // Reuse existing account if the email was registered manually before
        User user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(
                        User.builder()
                                .email(email)
                                .fullName(name)
                                .isVerified(true)   // Google already verified the email
                                .isActive(true)
                                .build()
                )
        );

        // Link the OAuth provider account in oauth_accounts table
        oauthAccountRepository.save(
                OAuthAccount.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .build()
        );

        // Create an empty profile row if this is a brand-new user
        if (userProfileRepository.findByUserId(user.getId()).isEmpty()) {
            userProfileRepository.save(
                    UserProfile.builder()
                            .user(user)
                            .fullName(name)
                            .build()
            );
        }

        return user;
    }
}
