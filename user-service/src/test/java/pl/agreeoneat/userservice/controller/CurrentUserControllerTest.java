package pl.agreeoneat.userservice.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.agreeoneat.userservice.config.security.RestSecurityErrorHandler;
import pl.agreeoneat.userservice.config.security.UserServiceSecurityConfig;
import pl.agreeoneat.userservice.security.KeycloakRealmRoleConverter;
import pl.agreeoneat.userservice.security.KeycloakRealmRolesExtractor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = CurrentUserController.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false"
		})
@Import({
		UserServiceSecurityConfig.class,
		RestSecurityErrorHandler.class,
		KeycloakRealmRolesExtractor.class,
		KeycloakRealmRoleConverter.class
})
class CurrentUserControllerTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-07-21T16:00:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-07-21T16:20:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void returnsInformationFromAuthenticatedUserToken() throws Exception {
		mockMvc.perform(get("/api/users/me")
					.with(jwt()
							.jwt(token -> token
									.header("alg", "RS256")
									.subject("user-id")
									.issuer("http://localhost:8081/realms/agreeoneat")
									.audience(List.of("agreeoneat-api"))
									.issuedAt(ISSUED_AT)
									.expiresAt(EXPIRES_AT)
									.claim("email", "mateusz@example.com")
									.claim("preferred_username", "mateusz@example.com")
									.claim("name", "Mateusz Kosowski")
									.claim("azp", "agreeoneat-mobile")
									.claim("realm_access", Map.of("roles", List.of("USER"))))
							.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("user-id"))
				.andExpect(jsonPath("$.email").value("mateusz@example.com"))
				.andExpect(jsonPath("$.displayName").value("Mateusz Kosowski"))
				.andExpect(jsonPath("$.clientId").value("agreeoneat-mobile"))
				.andExpect(jsonPath("$.audience[0]").value("agreeoneat-api"))
				.andExpect(jsonPath("$.roles[0]").value("USER"))
				.andExpect(jsonPath("$.algorithm").value("RS256"))
				.andExpect(jsonPath("$.tokenLifetimeSeconds").value(1200));
	}
}
