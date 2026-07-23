package pl.agreeoneat.userservice.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakRealmRoleConverterTest {

	private final KeycloakRealmRoleConverter roleConverter =
			new KeycloakRealmRoleConverter(new KeycloakRealmRolesExtractor());

	@Test
	void convertsKeycloakRolesToSpringAuthorities() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claim("realm_access", Map.of("roles", List.of("USER", "ROLE_ADMIN")))
				.build();

		Set<String> authorities = roleConverter.convert(jwt).stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toSet());

		assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), authorities);
	}
}
