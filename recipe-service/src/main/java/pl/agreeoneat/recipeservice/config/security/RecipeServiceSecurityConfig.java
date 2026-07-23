package pl.agreeoneat.recipeservice.config.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import pl.agreeoneat.recipeservice.security.KeycloakRealmRoleConverter;

@Configuration(proxyBeanMethods = false)
public class RecipeServiceSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			RestSecurityErrorHandler securityErrorHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/api/**").hasRole("USER")
						.anyRequest().denyAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRealmRoleConverter realmRoleConverter) {
		JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
		DelegatingJwtGrantedAuthoritiesConverter authoritiesConverter =
				new DelegatingJwtGrantedAuthoritiesConverter(scopeConverter, realmRoleConverter);

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}
}
