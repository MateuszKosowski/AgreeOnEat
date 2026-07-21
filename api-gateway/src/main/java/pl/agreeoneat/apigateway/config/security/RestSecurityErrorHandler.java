package pl.agreeoneat.apigateway.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		writeProblem(
				response,
				HttpStatus.UNAUTHORIZED,
				"Unauthorized",
				"A valid Bearer access token is required.");
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException exception) throws IOException {
		writeProblem(
				response,
				HttpStatus.FORBIDDEN,
				"Forbidden",
				"The authenticated principal is not allowed to access this resource.");
	}

	private void writeProblem(
			HttpServletResponse response,
			HttpStatus status,
			String title,
			String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("""
				{"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
				""".formatted(title, status.value(), detail).strip());
	}
}
