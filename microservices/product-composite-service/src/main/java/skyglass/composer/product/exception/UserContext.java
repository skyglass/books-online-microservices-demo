package skyglass.composer.product.exception;

import org.springframework.security.core.Authentication;

public interface UserContext {
	String getUsernameFromCtx() throws Exception;

	String getUsernameFromAuthentication(Authentication authentication);
}
