package se.magnus.microservices.core.product.services;

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

@Component
public class ProductIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(ProductIntegration.class);

	private final String productServiceUrl = "http://product/api";

	private final ObjectMapper mapper;

	private WebClient authorizedWebClient;

	private final int productServiceTimeoutSec;

	@Autowired
	public ProductIntegration(
			ObjectMapper mapper,
			@Value("${app.product-service.timeoutSec}") int productServiceTimeoutSec,
			WebClient webClient

	) {
		this.mapper = mapper;
		this.productServiceTimeoutSec = productServiceTimeoutSec;
		this.authorizedWebClient = webClient;
	}

	//@Retry(name = "product")
	//@CircuitBreaker(name = "product")
	public Mono<Product> getProduct(HttpHeaders headers, int productId, int delay, int faultPercent) {

		URI url = UriComponentsBuilder.fromUriString(productServiceUrl + "/product-ext/{productId}").build(productId);
		LOG.debug("Will call the getProduct API on URL: {}", url);

		return getAuthorizedWebClient().get().uri(url)
				.headers(h -> h.addAll(headers))
				.retrieve().bodyToMono(Product.class).log(null, FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex))
				.timeout(Duration.ofSeconds(productServiceTimeoutSec));
	}

	private WebClient getAuthorizedWebClient() {
		return authorizedWebClient;
	}

	private Throwable handleException(Throwable ex) {

		if (!(ex instanceof WebClientResponseException)) {
			LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
			return ex;
		}

		WebClientResponseException wcre = (WebClientResponseException) ex;

		switch (wcre.getStatusCode()) {

			case NOT_FOUND:
				return new NotFoundException(getErrorMessage(wcre));

			case UNPROCESSABLE_ENTITY:
				return new InvalidInputException(getErrorMessage(wcre));

			default:
				LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
				LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
				return ex;
		}
	}

	private String getErrorMessage(WebClientResponseException ex) {
		try {
			return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
		} catch (IOException ioex) {
			return ex.getMessage();
		}
	}
}
