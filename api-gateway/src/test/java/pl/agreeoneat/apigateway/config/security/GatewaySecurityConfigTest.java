package pl.agreeoneat.apigateway.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.agreeoneat.apigateway.testsupport.SecurityTestController;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = SecurityTestController.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false",
				"agreeoneat.security.cors.allowed-origins=http://localhost:3000"
		})
@Import({GatewaySecurityConfig.class, GatewayCorsConfig.class, RestSecurityErrorHandler.class})
class GatewaySecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsProtectedEndpointWithoutToken() throws Exception {
		mockMvc.perform(get("/api/test"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void acceptsProtectedEndpointWithAuthenticatedJwt() throws Exception {
		mockMvc.perform(get("/api/test").with(jwt().jwt(token -> token.subject("user-id"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void rejectsInvalidBearerToken() throws Exception {
		when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

		mockMvc.perform(get("/api/test")
					.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void allowsHealthEndpointWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void deniesUnknownEndpointForAuthenticatedPrincipal() throws Exception {
		mockMvc.perform(get("/internal").with(jwt()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void allowsCorsPreflightFromLocalFrontend() throws Exception {
		mockMvc.perform(options("/api/test")
					.header(HttpHeaders.ORIGIN, "http://localhost:3000")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	@Test
	void rejectsCorsPreflightFromUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/test")
					.header(HttpHeaders.ORIGIN, "http://localhost:5173")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
