package pl.agreeoneat.userservice.dto;

import java.time.Instant;
import java.util.List;

public record CurrentUserResponse(
		String subject,
		String email,
		String preferredUsername,
		String displayName,
		String clientId,
		List<String> audience,
		List<String> roles,
		String issuer,
		String algorithm,
		Instant issuedAt,
		Instant expiresAt,
		Long tokenLifetimeSeconds) {
}
