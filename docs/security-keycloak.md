# Konfiguracja Keycloak w AgreeOnEat

Ten dokument opisuje wyłącznie aktualną, wersjonowaną konfigurację Keycloak używaną lokalnie przez AgreeOnEat. Pełny przebieg logowania, wystawiania tokenów, wywołania API Gateway i walidacji JWT w mikroserwisach znajduje się w [`security-flow.md`](security-flow.md).

## Podstawowe pojęcia

- **Keycloak** jest systemem odpowiedzialnym za rejestrację, logowanie, przechowywanie hashy haseł, utrzymywanie sesji i wystawianie tokenów. Mikroserwisy AgreeOnEat nie otrzymują haseł użytkowników.
- **Realm** jest odizolowaną przestrzenią Keycloak posiadającą własnych użytkowników, klientów, role, sesje i ustawienia logowania. AgreeOnEat korzysta z realmu `agreeoneat`; realm `master` służy do administrowania serwerem Keycloak.
- **Klient OpenID Connect** reprezentuje aplikację, która rozpoczyna logowanie. Klient `agreeoneat-mobile` reprezentuje aplikację mobilną oraz Postmana używanego do lokalnych testów.
- **`client_id`** jest publicznym identyfikatorem klienta. Można go porównać do loginu aplikacji w Keycloak. W AgreeOnEat ma wartość `agreeoneat-mobile`.
- **`client_secret`** jest poufnym poświadczeniem klienta, podobnym do hasła aplikacji. Klient mobilny go nie posiada, ponieważ sekret zapisany w aplikacji instalowanej na urządzeniu można odczytać.
- **Klient publiczny** to klient bez `client_secret`. Do zabezpieczenia Authorization Code Flow wykorzystuje PKCE.
- **PKCE** wiąże authorization code z urządzeniem, które rozpoczęło logowanie. Dzięki temu sam przechwycony authorization code nie wystarcza do uzyskania tokenów.
- **Redirect URI** jest dokładnym adresem, pod który Keycloak może odesłać przeglądarkę po zakończeniu logowania. Musi zgadzać się z jednym z adresów zapisanych w konfiguracji klienta.
- **Client scope** w Keycloak jest zestawem mapperów i ustawień określających zawartość tokenu. Nie musi oznaczać biznesowej permisji.
- **Mapper protokołu** pobiera określoną informację z Keycloak i umieszcza ją w wybranym claimie tokenu, na przykład adres e-mail w `email` albo role w `realm_access.roles`.
- **Claim** jest pojedynczym polem wewnątrz tokenu, na przykład `sub`, `iss`, `aud` albo `exp`.
- **Audience (`aud`)** wskazuje API, dla którego wystawiono access token. Token AgreeOnEat przeznaczony dla backendu zawiera `agreeoneat-api`.
- **Rola realmu** opisuje ogólne uprawnienie użytkownika w danym realmie. Aktualna konfiguracja definiuje rolę `USER`.
- **Access token** jest przekazywany do API jako Bearer token. **ID token** opisuje wynik logowania i tożsamość użytkownika, natomiast **refresh token** służy do uzyskania kolejnego zestawu tokenów.
- **JWKS** jest publicznym zestawem kluczy Keycloak. API Gateway i mikroserwisy używają go do sprawdzania podpisów JWT; JWKS nie zawiera klucza prywatnego.

## Źródło konfiguracji

Źródłem prawdy dla realmu jest [`keycloak/realm-agreeoneat.json`](../keycloak/realm-agreeoneat.json). Plik zawiera ustawienia realmu, klienta OIDC, scope’ów, mapperów, ról, tokenów, sesji i haseł.

Docker Compose:

- montuje plik do `/opt/keycloak/data/import/realm-agreeoneat.json` w trybie tylko do odczytu;
- uruchamia Keycloak poleceniem `start-dev --import-realm`;
- przechowuje bieżący stan Keycloak w PostgreSQL na wolumenie `keycloak-db-data`;
- nie publikuje portu PostgreSQL poza sieć Compose.

Import tworzy realm tylko wtedy, gdy jeszcze nie istnieje. Zmiany wykonane w Admin Console są zapisywane w PostgreSQL i nie aktualizują automatycznie pliku JSON. Ponowne odtworzenie realmu z pliku wymaga pustej bazy Keycloak; usunięcie jej wolumenu usuwa również wszystkich lokalnych użytkowników.

Hasła administratora i bazy Keycloak pochodzą z ignorowanego pliku `.env` przez `KEYCLOAK_ADMIN_PASSWORD` i `KEYCLOAK_DB_PASSWORD`. Nie są częścią pliku realmu. W JSON-ie nie mogą znaleźć się również użytkownicy, hasła, hashe haseł, tokeny, sekrety klientów ani prywatne klucze.

## Realm

Realm jest odizolowaną przestrzenią Keycloak posiadającą własnych użytkowników, klientów, role, sesje i zasady logowania.

| Ustawienie | Wartość |
| --- | --- |
| Techniczna nazwa realmu | `agreeoneat` |
| Nazwa wyświetlana | `AgreeOnEat` |
| Realm aktywny | tak |
| Algorytm podpisu tokenów | `RS256` |
| Samodzielna rejestracja | włączona |
| E-mail jako username | włączony |
| Logowanie e-mailem | włączone |
| Duplikaty adresów e-mail | zabronione |
| Edycja username | wyłączona |
| Weryfikacja e-maila | wyłączona |
| Reset hasła przez e-mail | wyłączony |
| Remember Me | wyłączone |
| Ochrona brute-force | niewłączona w wersjonowanej konfiguracji |

Realm `master` nie przechowuje użytkowników AgreeOnEat. Służy do administrowania samym serwerem Keycloak.

## Lokalne adresy

| Element | Adres |
| --- | --- |
| Admin Console | `http://localhost:8081/admin` |
| OIDC Discovery | `http://localhost:8081/realms/agreeoneat/.well-known/openid-configuration` |
| Authorization endpoint | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/auth` |
| Token endpoint | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/token` |
| UserInfo endpoint | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/userinfo` |
| JWKS endpoint | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs` |

OIDC Discovery publikuje adresy endpointów i obsługiwane możliwości protokołu. JWKS udostępnia publiczne klucze potrzebne do sprawdzania podpisów tokenów. Prywatny klucz pozostaje w Keycloak.

## Klient `agreeoneat-mobile`

Klient reprezentuje aplikację mobilną AgreeOnEat i Postmana używanego do lokalnych testów. Jest klientem publicznym, ponieważ aplikacja instalowana na urządzeniu nie może bezpiecznie przechowywać współdzielonego `client_secret`.

| Ustawienie | Wartość |
| --- | --- |
| Client ID | `agreeoneat-mobile` |
| Protokół | OpenID Connect |
| Client authentication | wyłączone |
| Public client | tak |
| Standard Flow | włączony |
| PKCE | wymagane `S256` |
| Implicit Flow | wyłączony |
| Direct Access Grants | wyłączone |
| Service Accounts | wyłączone |
| Device Authorization Grant | wyłączony |
| CIBA Grant | wyłączony |
| Full Scope Allowed | wyłączone |
| Web origins | brak |

PKCE wiąże authorization code z jednorazowym `code_verifier` utworzonym przez klienta. Szczegółowy przebieg tego mechanizmu jest opisany w [`security-flow.md`](security-flow.md).

### Redirect URI

Dozwolone są dokładnie dwa callbacki:

| Zastosowanie | Redirect URI |
| --- | --- |
| Aplikacja mobilna | `com.agreeoneat://oauth/callback` |
| Lokalny test w Postmanie | `https://oauth.pstmn.io/v1/callback` |

Post-logout redirect URI jest ustawiony wyłącznie na:

```text
com.agreeoneat://oauth/callback
```

`com.agreeoneat://` jest własnym schematem URI obsługiwanym przez aplikację mobilną, a nie domeną internetową. Konfiguracja nie zawiera wildcardów.

## Client scopes i mappery

Klient ma następujące domyślne client scope’y:

```text
basic
profile
email
roles
agreeoneat-api
```

Nie ma optional client scope’ów. Każdy z obecnych scope’ów konfiguruje zawartość tokenów:

| Client scope | Mappery i wynik |
| --- | --- |
| `basic` | Dodaje `sub` do access tokenu oraz `auth_time` do access tokenu i ID tokenu. |
| `profile` | Dodaje `preferred_username`, `given_name`, `family_name` i `name` do access tokenu, ID tokenu oraz UserInfo. |
| `email` | Dodaje `email` i `email_verified` do access tokenu, ID tokenu oraz UserInfo. |
| `roles` | Dodaje role realmu do `realm_access.roles` w access tokenie. |
| `agreeoneat-api` | Dodaje `agreeoneat-api` do claima `aud` w access tokenie. |

Client scope w Keycloak jest pakietem mapperów definiujących zawartość tokenu. Nie oznacza automatycznie biznesowej permisji o tej samej nazwie.

## Rola `USER`

Realm definiuje jedną rolę aplikacyjną:

```text
USER
```

`USER` znajduje się w `defaultRoles`, dlatego otrzymuje ją każdy nowo utworzony użytkownik. `scopeMappings` klienta `agreeoneat-mobile` dopuszcza tę rolę, a wyłączone `Full Scope Allowed` zapobiega automatycznemu przekazywaniu wszystkich ról realmu.

Mapper client scope’u `roles` umieszcza rolę w access tokenie:

```json
{
  "realm_access": {
    "roles": ["USER"]
  }
}
```

Role związane z konkretnymi danymi biznesowymi, na przykład członkostwem w gospodarstwie, nie są rolami realmu Keycloak.

## Tokeny i sesje

| Ustawienie | Wartość |
| --- | --- |
| Algorytm podpisu | `RS256` |
| Ważność access tokenu | 1200 sekund, czyli 20 minut |
| SSO Session Idle | 604800 sekund, czyli 7 dni |
| SSO Session Max | 2592000 sekund, czyli 30 dni |
| Rotacja refresh tokenów | włączona |
| Refresh Token Max Reuse | `0` |
| Offline access | nieprzypisany do klienta |

Wygaśnięcie access tokenu nie kończy automatycznie sesji SSO. Możliwość używania refresh tokenu ograniczają czasy `SSO Session Idle` i `SSO Session Max`. Przy odświeżeniu Keycloak zwraca nowy refresh token i unieważnia poprzedni.

Konfiguracja wystawia trzy rodzaje tokenów:

- access token przeznaczony dla API AgreeOnEat;
- ID token opisujący wynik logowania i tożsamość użytkownika;
- refresh token używany wyłącznie na token endpoincie Keycloak.

Oczekiwane najważniejsze claimy access tokenu:

```json
{
  "iss": "http://localhost:8081/realms/agreeoneat",
  "sub": "stabilny-identyfikator-uzytkownika",
  "azp": "agreeoneat-mobile",
  "aud": "agreeoneat-api",
  "realm_access": {
    "roles": ["USER"]
  },
  "iat": 1700000000,
  "exp": 1700001200
}
```

`iat` i `exp` są znacznikami czasu Unix. Różnica między nimi wynosi 1200 sekund.

## Hasła

Wersjonowana polityka ma dokładną postać:

```text
hashAlgorithm(argon2) and length(12) and notUsername(undefined) and notEmail(undefined)
```

Oznacza to:

- haszowanie hasła algorytmem Argon2;
- co najmniej 12 znaków;
- zakaz użycia username jako hasła;
- zakaz użycia adresu e-mail jako hasła;
- brak wymuszonego okresowego wygasania hasła.

Argon2 przechowuje osolony hash z parametrami kosztu, a nie odwracalną postać hasła. Parametry kosztu nie są przypięte w realm JSON i pochodzą z domyślnych ustawień używanej wersji Keycloak.

W obecnej konfiguracji nie działa wysyłanie wiadomości SMTP, weryfikacja adresu e-mail, reset hasła przez e-mail ani ochrona brute-force. Jest to konfiguracja lokalnego środowiska deweloperskiego.

## Test konfiguracji w Postmanie

W kolekcji Postmana ustaw `Authorization` na `OAuth 2.0`, wyłącz `Share Token` i skonfiguruj:

| Pole Postmana | Wartość |
| --- | --- |
| Grant Type | `Authorization Code (With PKCE)` |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Auth URL | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/auth` |
| Access Token URL | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/token` |
| Client ID | `agreeoneat-mobile` |
| Client Secret | puste |
| Code Challenge Method | `SHA-256` |
| Code Verifier | puste, generowane przez Postmana |
| Scope | `openid` |
| Client Authentication | `Send client credentials in body` |

Zaznacz `Authorize using browser`, wybierz `Get new access token`, a po zalogowaniu lub rejestracji użyj `Proceed` i `Use Token`.

Token można sprawdzić żądaniem:

```http
GET http://localhost:8081/realms/agreeoneat/protocol/openid-connect/userinfo
Authorization: Bearer <access_token>
```

Oczekiwany wynik to `200 OK` i co najmniej claim `sub`. Po zmianie client scope’ów lub mapperów trzeba pobrać nowy token, ponieważ już wystawiony JWT nie zmienia swojej zawartości.

Access tokenów, refresh tokenów i haseł nie należy zapisywać w synchronizowanej kolekcji Postmana ani umieszczać na zrzutach ekranu.
