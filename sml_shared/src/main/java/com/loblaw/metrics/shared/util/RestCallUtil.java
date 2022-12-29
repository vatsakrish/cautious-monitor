package com.loblaw.metrics.shared.util;

import org.springframework.http.HttpEntity;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class RestCallUtil {
	public <T> Mono<T> getWebClient(String endpoint, T t, HttpEntity<String> entity, WebClient webClient,
			WebClientFlatMapInterface<T> wCFM, WebClientErrorInterface<T> wCE) {
		return webClient.get().uri(endpoint).headers(headers -> headers.addAll(entity.getHeaders()))
				.exchangeToMono(clientResponse -> wCFM.flatMap(clientResponse))
				.onErrorResume(error -> wCE.onError(error));
	}
}
