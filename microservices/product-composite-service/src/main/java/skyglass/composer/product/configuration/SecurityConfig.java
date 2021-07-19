package skyglass.composer.product.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableWebSecurity
@EnableOAuth2Client
class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.cors().and()
				.httpBasic().disable()
				.formLogin().disable()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
				.authorizeRequests()
				.antMatchers("/api/product/**").authenticated()
				.anyRequest().permitAll()
				.and()
				.oauth2ResourceServer()
				.jwt();

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
