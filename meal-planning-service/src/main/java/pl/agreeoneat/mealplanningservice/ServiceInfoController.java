package pl.agreeoneat.mealplanningservice;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plans")
public class ServiceInfoController {

	@GetMapping("/hello")
	Map<String, String> hello() {
		return Map.of("service", "meal-planning-service", "status", "ok");
	}
}
