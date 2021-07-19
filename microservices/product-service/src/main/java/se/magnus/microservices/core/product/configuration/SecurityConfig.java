package se.magnus.microservices.core.product.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securitygWebFilterChain(
			ServerHttpSecurity http) {
		http
				.cors().and()
				.httpBasic().disable()
				.formLogin().disable()
				.authorizeExchange()
				.pathMatchers("/api/product/**").authenticated()
				.anyExchange().permitAll()
				.and()
				.oauth2ResourceServer()
				.jwt();

		return http.build();
	}

}
