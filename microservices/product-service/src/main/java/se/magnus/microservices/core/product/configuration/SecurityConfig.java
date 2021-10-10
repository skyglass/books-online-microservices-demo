package se.magnus.microservices.core.product.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

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

	@Bean
	WebClient webClient(AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager reactiveClientManager) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(reactiveClientManager);
		oauth.setDefaultOAuth2AuthorizedClient(true);
		return WebClient.builder()
				.filter(oauth)
				.build();
	}

	@Bean
	public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties oAuth2ClientProperties) {
		List<ClientRegistration> clientRegistrations = new ArrayList<>();

		// because autoconfigure does not work for an unknown reason, here the ClientRegistrations are manually configured based on the application.yml
		oAuth2ClientProperties.getRegistration()
				.forEach((k, v) -> {
					Provider provider = oAuth2ClientProperties.getProvider().get(k);
					ClientRegistration clientRegistration = ClientRegistration
							.withRegistrationId(k)
							.tokenUri(provider.getTokenUri())
							.clientId(v.getClientId())
							.clientSecret(v.getClientSecret())
							.redirectUri(v.getRedirectUri())
							.authorizationUri(provider.getAuthorizationUri())
							.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
							.build();
					clientRegistrations.add(clientRegistration);
				});

		return new InMemoryReactiveClientRegistrationRepository(clientRegistrations);
	}

	@Bean
	AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager reactiveClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		ReactiveOAuth2AuthorizedClientService authorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
		return new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository,
				authorizedClientService);
	}

}
