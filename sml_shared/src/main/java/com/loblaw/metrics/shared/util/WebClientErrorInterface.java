package com.loblaw.metrics.shared.util;

import reactor.core.publisher.Mono;

public interface WebClientErrorInterface<T> {
	public Mono<T> onError(Throwable t);
}
