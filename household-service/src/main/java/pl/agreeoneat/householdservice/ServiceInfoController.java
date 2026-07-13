package pl.agreeoneat.householdservice;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households")
public class ServiceInfoController {

	@GetMapping("/hello")
	Map<String, String> hello() {
		return Map.of("service", "household-service", "status", "ok");
	}
}
