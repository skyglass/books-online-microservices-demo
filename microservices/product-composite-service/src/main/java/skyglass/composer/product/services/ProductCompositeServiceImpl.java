package skyglass.composer.product.services;

import static java.util.logging.Level.FINE;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.opentracing.Span;
import io.opentracing.Tracer;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.ProductCompositeService;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.composite.product.ServiceAddresses;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import skyglass.composer.product.configuration.SecurityContextUtils;

@RestController
@RequestMapping("/api")
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

	private final ServiceUtil serviceUtil;

	private final ProductCompositeIntegration integration;

	private Tracer tracer;

	@Autowired
	public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration, Tracer tracer) {
		this.serviceUtil = serviceUtil;
		this.integration = integration;
		this.tracer = tracer;
	}

	@Override
	public void createCompositeProduct(ProductAggregate body) {
		internalCreateCompositeProduct(SecurityContextHolder.getContext(), body);
	}

	public void internalCreateCompositeProduct(SecurityContext sc, ProductAggregate body) {

		try {

			SecurityContextUtils.logAuthorizationInfo(sc, LOG);

			LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

			Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
			integration.createProduct(product);

			if (body.getRecommendations() != null) {
				body.getRecommendations().forEach(r -> {
					Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
					integration.createRecommendation(recommendation);
				});
			}

			if (body.getReviews() != null) {
				body.getReviews().forEach(r -> {
					Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
					integration.createReview(review);
				});
			}

			LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

		} catch (RuntimeException re) {
			LOG.warn("createCompositeProduct failed: {}", re.toString());
			throw re;
		}
	}

	@Override
	public ProductAggregate getCompositeProduct(HttpHeaders requestHeaders, int productId) {

		LOG.info("Will get composite product info for product.id={} and username={}", productId, SecurityContextUtils.getUserName());
		Span span = tracer.activeSpan();
		span.log(String.format("Will get composite product info for product.id=%s and username=%s", productId, SecurityContextUtils.getUserName()));
		span.setTag("username", SecurityContextUtils.getUserName());

		HttpHeaders headers = getHeaders(requestHeaders, "X-group");
		SecurityContext sc = SecurityContextHolder.getContext();

		return Mono.zip(
				values -> createProductAggregate((SecurityContext) values[0], (Product) values[1], (List<Recommendation>) values[2], (List<Review>) values[3], serviceUtil.getServiceAddress()),
				Mono.just(sc),
				integration.getProduct(headers, productId, 0, 0)
						.onErrorReturn(CallNotPermittedException.class, getProductFallbackValue(productId)),
				integration.getRecommendations(headers, productId).collectList(),
				integration.getReviews(headers, productId).collectList())
				.doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
				.log(null, FINE).block();
	}

	@Override
	public void deleteCompositeProduct(int productId) {
		internalDeleteCompositeProduct(SecurityContextHolder.getContext(), productId);
	}

	private void internalDeleteCompositeProduct(SecurityContext sc, int productId) {
		try {
			SecurityContextUtils.logAuthorizationInfo(sc, LOG);

			LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

			integration.deleteProduct(productId);
			integration.deleteRecommendations(productId);
			integration.deleteReviews(productId);

			LOG.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);

		} catch (RuntimeException re) {
			LOG.warn("deleteCompositeProduct failed: {}", re.toString());
			throw re;
		}
	}

	private HttpHeaders getHeaders(HttpHeaders requestHeaders, String... headers) {
		LOG.trace("Will look for {} headers: {}", headers.length, headers);
		HttpHeaders h = new HttpHeaders();
		for (String header : headers) {
			List<String> value = requestHeaders.get(header);
			if (value != null) {
				h.addAll(header, value);
			}
		}
		h.add("Authorization", requestHeaders.getFirst("authorization"));
		LOG.trace("Will transfer {}, headers: {}", h.size(), h);
		return h;
	}

	/**
	 * Note that this method is called by Mono.onErrorReturn() in getCompositeProduct().
	 * Mono.onErrorReturn() will call this method once per execution to prepare a static response if the execution fails.
	 * Do not execute any lengthy or CPU intensive operation in this method.
	 *
	 * @param productId
	 * @return
	 */
	private Product getProductFallbackValue(int productId) {

		if (productId < 1)
			throw new InvalidInputException("Invalid productId: " + productId);

		if (productId == 13) {
			String errMsg = "Product Id: " + productId + " not found in fallback cache!";
			LOG.warn(errMsg);
			throw new NotFoundException(errMsg);
		}

		return new Product(productId, "Fallback product" + productId, productId, serviceUtil.getServiceAddress());
	}

	private ProductAggregate createProductAggregate(SecurityContext sc, Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

		SecurityContextUtils.logAuthorizationInfo(sc, LOG);

		// 1. Setup product info
		int productId = product.getProductId();
		String name = product.getName();
		int weight = product.getWeight();

		// 2. Copy summary recommendation info, if available
		List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null
				: recommendations.stream()
						.map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
						.collect(Collectors.toList());

		// 3. Copy summary review info, if available
		List<ReviewSummary> reviewSummaries = (reviews == null) ? null
				: reviews.stream()
						.map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
						.collect(Collectors.toList());

		// 4. Create info regarding the involved microservices addresses
		String productAddress = product.getServiceAddress();
		String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
		String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
		ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

		return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
	}

}
