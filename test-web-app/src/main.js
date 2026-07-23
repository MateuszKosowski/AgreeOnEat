import Keycloak from "keycloak-js";
import "./styles.css";

const trimTrailingSlash = (value) => value.replace(/\/+$/, "");

const config = Object.freeze({
  keycloakUrl: trimTrailingSlash(
    import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8081",
  ),
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "agreeoneat",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "agreeoneat-web-test",
  apiGatewayUrl: trimTrailingSlash(
    import.meta.env.VITE_API_GATEWAY_URL ?? "http://localhost:8080",
  ),
});

const appUrl = `${window.location.origin}/`;
const meEndpoint = `${config.apiGatewayUrl}/api/users/me`;

const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.realm,
  clientId: config.clientId,
});

const statusElement = document.querySelector("#status");
const errorElement = document.querySelector("#error");
const anonymousActions = document.querySelector("#anonymous-actions");
const authenticatedContent = document.querySelector("#authenticated-content");
const userResponse = document.querySelector("#user-response");
const loginButton = document.querySelector("#login-button");
const registerButton = document.querySelector("#register-button");
const logoutButton = document.querySelector("#logout-button");

function setStatus(message, variant) {
  statusElement.textContent = message;
  statusElement.className = `status status--${variant}`;
}

function showError(message) {
  errorElement.textContent = message;
  errorElement.hidden = false;
}

function clearError() {
  errorElement.textContent = "";
  errorElement.hidden = true;
}

function showAnonymousView() {
  anonymousActions.hidden = false;
  authenticatedContent.hidden = true;
  userResponse.textContent = "";
  setStatus("Użytkownik nie jest zalogowany.", "neutral");
}

function showAuthenticatedView() {
  anonymousActions.hidden = true;
  authenticatedContent.hidden = false;
  setStatus("Logowanie zakończone. Pobieram dane użytkownika…", "success");
}

async function readResponseBody(response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json") || contentType.includes("application/problem+json")) {
    return response.json();
  }

  return response.text();
}

async function loadCurrentUser() {
  clearError();
  await keycloak.updateToken(30);

  const response = await fetch(meEndpoint, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${keycloak.token}`,
    },
  });

  const body = await readResponseBody(response);

  if (!response.ok) {
    const details = typeof body === "string" ? body : JSON.stringify(body);
    throw new Error(`Backend zwrócił ${response.status}: ${details}`);
  }

  userResponse.textContent = JSON.stringify(body, null, 2);
  setStatus("Użytkownik jest zalogowany, a backend zaakceptował access token.", "success");
}

loginButton.addEventListener("click", () => {
  clearError();
  keycloak.login({ redirectUri: appUrl });
});

registerButton.addEventListener("click", () => {
  clearError();
  keycloak.register({ redirectUri: appUrl });
});

logoutButton.addEventListener("click", () => {
  clearError();
  keycloak.logout({ redirectUri: appUrl });
});

keycloak.onTokenExpired = async () => {
  try {
    await keycloak.updateToken(30);
  } catch {
    keycloak.clearToken();
    showAnonymousView();
    showError("Sesja wygasła. Zaloguj się ponownie.");
  }
};

try {
  const authenticated = await keycloak.init({
    checkLoginIframe: false,
    flow: "standard",
    pkceMethod: "S256",
    redirectUri: appUrl,
  });

  if (!authenticated) {
    showAnonymousView();
  } else {
    showAuthenticatedView();
    await loadCurrentUser();
  }
} catch (error) {
  showAnonymousView();
  showError(
    error instanceof Error
      ? error.message
      : "Nie udało się zainicjalizować logowania przez Keycloak.",
  );
}
