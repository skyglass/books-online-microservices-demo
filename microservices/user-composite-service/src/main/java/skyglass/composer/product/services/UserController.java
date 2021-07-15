package skyglass.composer.product.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

// import org.springframework.security.oauth2.client.annotation.OAuth2Client;

@RestController
@RequestMapping("/api/user")
@Api(description = "REST API for user information.")
public class UserController {

	@Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
	private String tokenUri;

	@ApiOperation(value = "JWT Token")
	@GetMapping(value = "/jwt-token", produces = "text/html")
	public String jwtToken(@RequestParam String username, @RequestParam String password) {
		String jwtToken = WebClient.builder()
				.build()
				.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(
						"grant_type", "password")
						.with("client_id", clientId)
						.with("username", username)
						.with("password", password))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.flatMap(tokenResponse -> {
					String result = tokenResponse.get("access_token")
							.textValue();
					return Mono.just(result);
				}).block();
		return "Bearer " + jwtToken;
	}

}
