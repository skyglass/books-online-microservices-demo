package skyglass.composer.product.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * SecurityContextUtils is used to get username and roles to set created by, last updated by fields.
 */
@Component
public class SecurityContextUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityContextUtils.class);

	private static final String ANONYMOUS = "anonymous";

	private SecurityContextUtils() {
	}

	public static String getUserName() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		String username = ANONYMOUS;
		if (authentication != null) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof Jwt) {
				Jwt jwt = (Jwt) principal;
				username = jwt.getClaimAsString("preferred_username");
			} else if (principal instanceof String) {
				username = (String) principal;
			}
		}
		return username;
	}

	public static Map<String, Object> getUserAttributes() {
		Map<String, Object> result = new HashMap<>();
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		String username = ANONYMOUS;
		if (authentication != null) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof Jwt) {
				Jwt jwt = (Jwt) principal;
				username = jwt.getClaimAsString("preferred_username");
				result.putAll(jwt.getClaims());
			} else if (principal instanceof String) {
				username = (String) principal;
			}
		}
		result.put("username", username);
		result.put("roles", SecurityContextUtils.getUserRoles());
		return result;
	}

	public static Set<String> getUserRoles() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		Set<String> roles = new HashSet<>();

		if (null != authentication) {
			authentication.getAuthorities()
					.forEach(e -> roles.add(e.getAuthority()));
		}
		return roles;
	}
}
