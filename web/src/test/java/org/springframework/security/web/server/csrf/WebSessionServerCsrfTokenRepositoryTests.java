/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.web.server.csrf;

import org.junit.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class WebSessionServerCsrfTokenRepositoryTests {
	private WebSessionServerCsrfTokenRepository repository = new WebSessionServerCsrfTokenRepository();

	private MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	@Test
	public void generateTokenThenNoSession() {
		Mono<CsrfToken> result = this.repository.generateToken(this.exchange);

		Mono<Boolean> isSessionStarted = this.exchange.getSession()
			.map(WebSession::isStarted);

		StepVerifier.create(isSessionStarted)
			.expectNext(false)
			.verifyComplete();
	}

	@Test
	public void generateTokenWhenSubscriptionThenNoSession() {
		Mono<CsrfToken> result = this.repository.generateToken(this.exchange);

		Mono<Boolean> isSessionStarted = this.exchange.getSession()
			.map(WebSession::isStarted);

		StepVerifier.create(isSessionStarted)
			.expectNext(false)
			.verifyComplete();
	}

	@Test
	public void generateTokenWhenGetTokenThenAddsToSession() {
		Mono<CsrfToken> result = this.repository.generateToken(this.exchange);
		result.block().getToken();

		WebSession session = this.exchange.getSession().block();
		Map<String, Object> attributes = session.getAttributes();

		assertThat(session.isStarted()).isTrue();
		assertThat(attributes).hasSize(1);
		assertThat(attributes.values().iterator().next()).isInstanceOf(CsrfToken.class);
	}

	@Test
	public void saveTokenWhenNullThenDeletes() {
		CsrfToken token = this.repository.generateToken(this.exchange).block();
		token.getToken();

		Mono<CsrfToken> result = this.repository.saveToken(this.exchange, null);
		StepVerifier.create(result)
			.verifyComplete();

		WebSession session = this.exchange.getSession().block();

		assertThat(session.getAttributes()).isEmpty();
	}

	@Test
	public void generateTokenAndLoadTokenDeleteTokenWhenNullThenDeletes() {
		CsrfToken generate = this.repository.generateToken(this.exchange).block();
		generate.getToken();

		CsrfToken load = this.repository.loadToken(this.exchange).block();
		assertThat(load).isEqualTo(generate);

		this.repository.saveToken(this.exchange, null).block();
		WebSession session = this.exchange.getSession().block();
		assertThat(session.getAttributes()).isEmpty();

		load = this.repository.loadToken(this.exchange).block();
		assertThat(load).isNull();
	}
}
