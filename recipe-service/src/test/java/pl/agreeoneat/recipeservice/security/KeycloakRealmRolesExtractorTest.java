package pl.agreeoneat.recipeservice.security;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakRealmRolesExtractorTest {

	private final KeycloakRealmRolesExtractor rolesExtractor = new KeycloakRealmRolesExtractor();

	@Test
	void extractsStringRolesFromRealmAccessClaim() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
				.build();

		assertEquals(List.of("USER", "ADMIN"), rolesExtractor.convert(jwt));
	}

	@Test
	void returnsEmptyListWhenRealmAccessClaimIsMissing() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject("user-id")
				.build();

		assertEquals(List.of(), rolesExtractor.convert(jwt));
	}
}
