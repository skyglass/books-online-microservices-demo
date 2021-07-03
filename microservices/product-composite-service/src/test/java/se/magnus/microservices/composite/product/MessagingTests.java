package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.http.HttpStatus.OK;
import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.api.event.Event.Type;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = { ProductCompositeServiceApplication.class, TestSecurityConfig.class }, properties = { "spring.main.allow-bean-definition-overriding=true" })
public class MessagingTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ProductCompositeIntegration.MessageSources channels;

	@Autowired
	private MessageCollector collector;

	BlockingQueue<Message<?>> queueProducts = null;

	BlockingQueue<Message<?>> queueRecommendations = null;

	BlockingQueue<Message<?>> queueReviews = null;

	@BeforeEach
	public void setUp() {
		queueProducts = getQueue(channels.outputProducts());
		queueRecommendations = getQueue(channels.outputRecommendations());
		queueReviews = getQueue(channels.outputReviews());
	}

	@Test
	public void createCompositeProduct1() {

		ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
		postAndVerifyProduct(composite, OK);

		// Assert one expected new product events queued up
		assertEquals(2, queueProducts.size());

		//TODO adapt for junit 5
		Event<Integer, Product> expectedEvent = new Event(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
		receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent));

		// Assert none recommendations and review events
		assertEquals(1, queueRecommendations.size());
		assertEquals(1, queueReviews.size());
	}

	@Test
	public void createCompositeProduct2() {

		ProductAggregate composite = new ProductAggregate(1, "name", 1,
				singletonList(new RecommendationSummary(1, "a", 1, "c")),
				singletonList(new ReviewSummary(1, "a", "s", "c")), null);

		postAndVerifyProduct(composite, OK);

		// Assert one create product event queued up
		assertEquals(3, queueProducts.size());

		Event<Integer, Product> expectedProductEvent = new Event(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
		receivesPayloadThat(sameEventExceptCreatedAt(expectedProductEvent));

		// Assert one create recommendation event queued up
		assertEquals(2, queueRecommendations.size());

		RecommendationSummary rec = composite.getRecommendations().get(0);
		Event<Integer, Product> expectedRecommendationEvent = new Event(CREATE, composite.getProductId(),
				new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null));
		receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent));

		// Assert one create review event queued up
		assertEquals(2, queueReviews.size());

		ReviewSummary rev = composite.getReviews().get(0);
		Event<Integer, Product> expectedReviewEvent = new Event(CREATE, composite.getProductId(),
				new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(), rev.getSubject(), rev.getContent(), null));
		receivesPayloadThat(sameEventExceptCreatedAt(expectedReviewEvent));
	}

	@Test
	public void deleteCompositeProduct() {
		deleteAndVerifyProduct(1, OK);

		// Assert one delete product event queued up
		assertEquals(1, queueProducts.size());

		Event<Integer, Product> expectedEvent = new Event(Type.DELETE, 1, null);
		receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent));

		// Assert one delete recommendation event queued up
		assertEquals(1, queueRecommendations.size());

		Event<Integer, Product> expectedRecommendationEvent = new Event(Type.DELETE, 1, null);
		receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent));

		// Assert one delete review event queued up
		assertEquals(1, queueReviews.size());

		Event<Integer, Product> expectedReviewEvent = new Event(Type.DELETE, 1, null);
		receivesPayloadThat(sameEventExceptCreatedAt(expectedReviewEvent));
	}

	private BlockingQueue<Message<?>> getQueue(MessageChannel messageChannel) {
		return collector.forChannel(messageChannel);
	}

	private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
		client.post()
				.uri("/product-composite")
				.body(just(compositeProduct), ProductAggregate.class)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus);
	}

	private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		client.delete()
				.uri("/product-composite/" + productId)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus);
	}
}
