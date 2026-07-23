package pl.agreeoneat.userservice.controller;

import java.time.Duration;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.agreeoneat.userservice.dto.CurrentUserResponse;
import pl.agreeoneat.userservice.security.KeycloakRealmRolesExtractor;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

	private final KeycloakRealmRolesExtractor rolesExtractor;

	public CurrentUserController(KeycloakRealmRolesExtractor rolesExtractor) {
		this.rolesExtractor = rolesExtractor;
	}

	@GetMapping("/me")
	CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return new CurrentUserResponse(
				jwt.getSubject(),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("preferred_username"),
				jwt.getClaimAsString("name"),
				jwt.getClaimAsString("azp"),
				jwt.getAudience(),
				rolesExtractor.convert(jwt),
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
}
