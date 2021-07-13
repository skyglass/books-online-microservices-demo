package skyglass.composer.product.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * SecurityConfigurer is to configure ResourceServer and HTTP Security.
 * <p>
 * Please make sure you check HTTP Security configuration and change is as per your needs.
 * </p>
 *
 * Note: Use {@link SecurityProperties} to configure required CORs configuration and enable or disable security of application.
 */
@Configuration
@EnableWebFluxSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//@ConditionalOnProperty(prefix = "rest.security", value = "enabled", havingValue = "true")
public class SecurityConfigurer {

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

		http.cors()
				.and()
				.headers()
				.frameOptions()
				.disable()
				.and()
				.csrf()
				.disable()
				.authorizeExchange()
				.pathMatchers("/actuator/**", "/swagger-ui/**", "/product-composite/actuator/**",
						"/product-composite/swagger-ui/**")
				.permitAll()
				//.pathMatchers(POST, "/product-composite/**").hasAuthority("SCOPE_product:write")
				//.pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
				//.pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
				.anyExchange().authenticated()
				//.and()
				//.exceptionHandling()
				//.accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.BAD_REQUEST))
				.and()
				.oauth2ResourceServer()
				.jwt();

		return http.build();

	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(4);
	}

}
