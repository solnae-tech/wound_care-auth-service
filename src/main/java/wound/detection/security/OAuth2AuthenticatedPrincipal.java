package wound.detection.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Wraps the raw {@link OAuth2User} returned by the provider and carries
 * the resolved local user's plain-value fields (id, email, fullName).
 *
 * <p>We deliberately store primitives — <strong>not</strong> the JPA entity —
 * because the Hibernate session that loaded the entity is closed by the time
 * {@link OAuth2SuccessHandler} runs. Storing the entity as a lazy proxy would
 * cause {@code LazyInitializationException} on the first field access.</p>
 */
public class OAuth2AuthenticatedPrincipal implements OAuth2User {

    private final OAuth2User delegate;

    // ── Plain values from the local User row (safe outside any session) ───────
    private final Long   userId;
    private final String email;
    private final String fullName;

    public OAuth2AuthenticatedPrincipal(OAuth2User delegate,
                                        Long       userId,
                                        String     email,
                                        String     fullName) {
        this.delegate  = delegate;
        this.userId    = userId;
        this.email     = email;
        this.fullName  = fullName;
    }

    // ── OAuth2User ────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return delegate.getName(); // provider sub/id
    }

    // ── Local user getters ────────────────────────────────────────────────────

    public Long   getUserId()  { return userId;   }
    public String getEmail()   { return email;    }
    public String getFullName(){ return fullName; }
}
