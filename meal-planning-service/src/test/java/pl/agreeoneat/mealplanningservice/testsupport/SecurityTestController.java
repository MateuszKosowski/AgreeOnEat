package pl.agreeoneat.mealplanningservice.testsupport;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTestController {

	@GetMapping("/api/test")
	Map<String, String> protectedEndpoint() {
		return Map.of("status", "UP");
	}

	@GetMapping("/actuator/health")
	Map<String, String> health() {
		return Map.of("status", "UP");
	}

	@GetMapping("/internal")
	Map<String, String> internal() {
		return Map.of("status", "UP");
	}
}
