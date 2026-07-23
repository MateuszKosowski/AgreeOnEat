package pl.agreeoneat.recipeservice.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRealmRolesExtractor implements Converter<Jwt, List<String>> {

	@Override
	public List<String> convert(Jwt jwt) {
		Object realmAccessClaim = jwt.getClaim("realm_access");
		if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
			return List.of();
		}

		Object rolesClaim = realmAccess.get("roles");
		if (!(rolesClaim instanceof Collection<?> roles)) {
			return List.of();
		}

		return roles.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.toList();
	}
}
