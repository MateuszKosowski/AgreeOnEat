package pl.agreeoneat.userservice.controller;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.agreeoneat.userservice.dto.CurrentUserResponse;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

	@GetMapping("/me")
	CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return new CurrentUserResponse(
				jwt.getSubject(),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("name"),
				jwt.getClaimAsString("azp"),
				jwt.getAudience(),
				extractRealmRoles(jwt),
				jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
				extractAlgorithm(jwt),
				jwt.getIssuedAt(),
				jwt.getExpiresAt(),
				extractTokenLifetimeSeconds(jwt));
	}

	private String extractAlgorithm(Jwt jwt) {
		Object algorithm = jwt.getHeaders().get("alg");
		return algorithm instanceof String value ? value : null;
	}

	private Long extractTokenLifetimeSeconds(Jwt jwt) {
		if (jwt.getIssuedAt() == null || jwt.getExpiresAt() == null) {
			return null;
		}
		return Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds();
	}

	private List<String> extractRealmRoles(Jwt jwt) {
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
