package se.magnus.util.reactor;

import static java.util.logging.Level.FINE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

public class ReactorTests {

	@Test
	public void TestFlux() {

		List<Integer> list = new ArrayList<>();

		Flux.just(1, 2, 3, 4)
				.filter(n -> n % 2 == 0)
				.map(n -> n * 2)
				.log(null, FINE)
				.subscribe(n -> list.add(n));

		assertThat(list).containsExactly(4, 8);
	}

	@Test
	public void TestFluxBlocking() {

		List<Integer> list = Flux.just(1, 2, 3, 4)
				.filter(n -> n % 2 == 0)
				.map(n -> n * 2)
				.log(null, FINE)
				.collectList().block();

		assertThat(list).containsExactly(4, 8);
	}

}
