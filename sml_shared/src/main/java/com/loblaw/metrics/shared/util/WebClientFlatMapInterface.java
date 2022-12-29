package com.loblaw.metrics.shared.util;

import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.publisher.Mono;

public interface WebClientFlatMapInterface<T> {
	public Mono<T> flatMap(ClientResponse clientResponse);
}
