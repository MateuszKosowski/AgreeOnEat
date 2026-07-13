package pl.agreeoneat.userservice;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class ServiceInfoController {

	@GetMapping("/hello")
	Map<String, String> hello() {
		return Map.of("service", "user-service", "status", "ok");
	}

	@GetMapping("/ping")
	Map<String, String> ping() {
		return Map.of("service", "user-service", "status", "UP");
	}
}
