package pl.agreeoneat.apigateway.config.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agreeoneat.security.cors")
public record GatewayCorsProperties(List<String> allowedOrigins) {

	public GatewayCorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}
}
