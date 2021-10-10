package se.magnus.microservices.core.product.services;

import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Mono.error;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.Span;
import io.opentracing.Tracer;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.microservices.core.product.configuration.SecurityContextUtils;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
@RequestMapping("/api")
public class ProductServiceImpl implements ProductService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

	private final ServiceUtil serviceUtil;

	private final ProductRepository repository;

	private final ProductMapper mapper;

	private final Tracer tracer;

	private final ProductIntegration productIntegration;

	@Autowired
	public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil, Tracer tracer, ProductIntegration productIntegration) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
		this.tracer = tracer;
		this.productIntegration = productIntegration;
	}

	@Override
	public Product createProduct(Product body) {

		if (body.getProductId() < 1)
			throw new InvalidInputException("Invalid productId: " + body.getProductId());

		ProductEntity entity = mapper.apiToEntity(body);
		Mono<Product> newEntity = repository.save(entity)
				.log(null, FINE)
				.onErrorMap(
						DuplicateKeyException.class,
						ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
				.map(e -> mapper.entityToApi(e));

		return newEntity.block();
	}

	public Mono<Product> getProductExt(HttpHeaders headers, int productId, int delay, int faultPercent) {
		return ReactiveSecurityContextHolder.getContext().flatMap(sc -> getProduct(sc, headers, productId, delay, faultPercent));
	}

	@Override
	public Mono<Product> getProduct(HttpHeaders headers, int productId, int delay, int faultPercent) {
		return productIntegration.getProduct(headers, productId, delay, faultPercent);
	}

	private Mono<Product> getProduct(SecurityContext sc, HttpHeaders headers, int productId, int delay, int faultPercent) {

		if (productId < 1)
			throw new InvalidInputException("Invalid productId: " + productId);

		if (delay > 0)
			simulateDelay(delay);

		if (faultPercent > 0)
			throwErrorIfBadLuck(faultPercent);

		Span span = tracer.activeSpan();
		span.log(String.format("Will get product info for product.id=%s and username=%s", productId, SecurityContextUtils.getUserName(sc)));
		span.setTag("username-reactive", SecurityContextUtils.getUserName(sc));

		LOG.info("Will get product info for product.id={} and username={}", productId, SecurityContextUtils.getUserName(sc));

		return repository.findByProductId(productId)
				.switchIfEmpty(error(new NotFoundException("No product found for productId: " + productId)))
				.log(null, FINE)
				.map(e -> mapper.entityToApi(e))
				.map(e -> {
					e.setServiceAddress(serviceUtil.getServiceAddress());
					return e;
				});
	}

	@Override
	public void deleteProduct(int productId) {

		if (productId < 1)
			throw new InvalidInputException("Invalid productId: " + productId);

		LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
		repository.findByProductId(productId).log(null, FINE).map(e -> repository.delete(e)).flatMap(e -> e).block();
	}

	private void simulateDelay(int delay) {
		LOG.debug("Sleeping for {} seconds...", delay);
		try {
			Thread.sleep(delay * 1000);
		} catch (InterruptedException e) {
		}
		LOG.debug("Moving on...");
	}

	private void throwErrorIfBadLuck(int faultPercent) {
		int randomThreshold = getRandomNumber(1, 100);
		if (faultPercent < randomThreshold) {
			LOG.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
		} else {
			LOG.warn("Bad luck, an error occurred, {} >= {}", faultPercent, randomThreshold);
			throw new RuntimeException("Something went wrong...");
		}
	}

	private final Random randomNumberGenerator = new Random();

	private int getRandomNumber(int min, int max) {

		if (max < min) {
			throw new RuntimeException("Max must be greater than min");
		}

		return randomNumberGenerator.nextInt((max - min) + 1) + min;
	}

}
