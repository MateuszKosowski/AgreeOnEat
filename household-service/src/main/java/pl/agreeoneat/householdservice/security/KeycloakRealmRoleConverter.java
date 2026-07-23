package pl.agreeoneat.householdservice.security;

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLE_PREFIX = "ROLE_";

	private final KeycloakRealmRolesExtractor rolesExtractor;

	public KeycloakRealmRoleConverter(KeycloakRealmRolesExtractor rolesExtractor) {
		this.rolesExtractor = rolesExtractor;
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		return rolesExtractor.convert(jwt).stream()
				.map(this::toAuthority)
				.toList();
	}

	private GrantedAuthority toAuthority(String role) {
		String authority = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
		return new SimpleGrantedAuthority(authority);
	}
}
