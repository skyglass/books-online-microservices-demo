package se.magnus.api.core.product;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface ProductService {

	Product createProduct(@RequestBody Product body);

	/**
	 * Sample usage: curl $HOST:$PORT/product/1
	 *
	 * @param productId
	 * @return the product, if found, else null
	 */
	@GetMapping(value = "/product/{productId}", produces = "application/json")
	Product getProduct(
			@RequestHeader HttpHeaders headers,
			@PathVariable int productId,
			@RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
			@RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent);

	void deleteProduct(@PathVariable int productId);
}
