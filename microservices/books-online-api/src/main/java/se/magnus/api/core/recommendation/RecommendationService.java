package se.magnus.api.core.recommendation;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import reactor.core.publisher.Flux;

public interface RecommendationService {

	Recommendation createRecommendation(@RequestBody Recommendation body);

	/**
	 * Sample usage:
	 *
	 * curl $HOST:$PORT/recommendation?productId=1
	 *
	 * @param productId
	 * @return
	 */
	@GetMapping(value = "/recommendation", produces = "application/json")
	Flux<Recommendation> getRecommendations(@RequestHeader HttpHeaders headers, @RequestParam(value = "productId", required = true) int productId);

	void deleteRecommendations(@RequestParam(value = "productId", required = true) int productId);
}
