package pl.agreeoneat.recipeservice.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.agreeoneat.recipeservice.security.KeycloakRealmRoleConverter;
import pl.agreeoneat.recipeservice.security.KeycloakRealmRolesExtractor;
import pl.agreeoneat.recipeservice.testsupport.SecurityTestController;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = SecurityTestController.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false"
		})
@Import({
		RecipeServiceSecurityConfig.class,
		RestSecurityErrorHandler.class,
		KeycloakRealmRolesExtractor.class,
		KeycloakRealmRoleConverter.class
})
class RecipeServiceSecurityConfigTest {

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
	void acceptsProtectedEndpointWithUserRole() throws Exception {
		mockMvc.perform(get("/api/test")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void rejectsProtectedEndpointWithoutUserRole() throws Exception {
		mockMvc.perform(get("/api/test").with(jwt()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.title").value("Forbidden"));
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
	void deniesUnknownEndpointForUser() throws Exception {
		mockMvc.perform(get("/internal")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.title").value("Forbidden"));
	}
}
