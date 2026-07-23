# AgreeOnEat test web app

Minimalny klient przeglądarkowy do lokalnego sprawdzenia pełnego przepływu:

```text
logowanie lub rejestracja w Keycloak
→ Authorization Code + PKCE S256
→ access token
→ API Gateway
→ GET /api/users/me
```

Aplikacja używa oficjalnego adaptera `keycloak-js`. Tokeny są przechowywane wyłącznie w pamięci adaptera i nie trafiają do `localStorage`.

## Uruchomienie

Cały stos wraz z aplikacją:

```powershell
docker compose up --build
```

Następnie otwórz:

```text
http://localhost:3000
```

Klient Keycloak `agreeoneat-web-test` musi istnieć w realmie. Jest częścią wersjonowanego pliku [`keycloak/realm-agreeoneat.json`](../keycloak/realm-agreeoneat.json). Import realmu nie nadpisuje jednak istniejącego realmu w bazie Keycloak; szczegóły znajdują się w [`docs/security-keycloak.md`](../docs/security-keycloak.md).

Na ekranie:

1. wybierz **Zarejestruj**, aby utworzyć użytkownika, albo **Zaloguj**, jeśli konto już istnieje;
2. formularz wyświetlony przez Keycloak obsłuży dane logowania;
3. po poprawnym callbacku aplikacja pobierze access token i wywoła `GET http://localhost:8080/api/users/me`;
4. odpowiedź JSON pojawi się na stronie.

## Konfiguracja

Compose przekazuje do Vite następujące wartości z ustawieniami lokalnymi:

| Zmienna w aplikacji | Zmienna nadpisująca w Compose | Domyślna wartość |
| --- | --- | --- |
| `VITE_KEYCLOAK_URL` | `TEST_WEB_KEYCLOAK_URL` | `http://localhost:8081` |
| `VITE_KEYCLOAK_REALM` | `TEST_WEB_KEYCLOAK_REALM` | `agreeoneat` |
| `VITE_KEYCLOAK_CLIENT_ID` | `TEST_WEB_KEYCLOAK_CLIENT_ID` | `agreeoneat-web-test` |
| `VITE_API_GATEWAY_URL` | `TEST_WEB_API_GATEWAY_URL` | `http://localhost:8080` |

Jeżeli lokalny wolumen Keycloak zawiera realm utworzony przed dodaniem klienta `agreeoneat-web-test`, sam restart go nie zaktualizuje. Można skonfigurować klienta według tabeli w [`docs/security-keycloak.md`](../docs/security-keycloak.md) albo — jeśli lokalni użytkownicy nie są potrzebni — odtworzyć Keycloak poleceniami `docker compose down -v` i `docker compose up -d --build`. Pierwsze polecenie usuwa całą lokalną bazę Keycloak.
