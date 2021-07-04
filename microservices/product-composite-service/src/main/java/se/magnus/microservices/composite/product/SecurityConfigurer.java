package se.magnus.microservices.composite.product;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

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
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(prefix = "rest.security", value = "enabled", havingValue = "true")
public class SecurityConfigurer extends ResourceServerConfigurerAdapter {

	private ResourceServerProperties resourceServerProperties;

	/* Using spring constructor injection, @Autowired is implicit */
	public SecurityConfigurer(ResourceServerProperties resourceServerProperties) {
		this.resourceServerProperties = resourceServerProperties;
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId(resourceServerProperties.getResourceId());
	}

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

		http.cors()
				.and()
				.headers()
				.frameOptions()
				.disable()
				.and()
				.csrf()
				.disable()
				.authorizeExchange()
				.pathMatchers("/actuator/**").permitAll()
				.pathMatchers(POST, "/product-composite/**").hasAuthority("SCOPE_product:write")
				.pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
				.pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
				.anyExchange().authenticated()
				.and()
				.exceptionHandling()
				.accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.BAD_REQUEST))
				.and()
				.oauth2ResourceServer()
				.jwt();

		return http.build();

	}

	@Bean
	public JwtAccessTokenCustomizer jwtAccessTokenCustomizer(ObjectMapper mapper) {
		return new JwtAccessTokenCustomizer(mapper);
	}

}
