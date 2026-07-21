# Konfiguracja bezpieczeństwa Keycloak

Ten dokument opisuje uzgodnioną lokalną konfigurację Keycloak dla AgreeOnEat. Wersjonowanym źródłem konfiguracji jest plik [`keycloak/realm-agreeoneat.json`](../keycloak/realm-agreeoneat.json).

## Podstawowe pojęcia

- **Keycloak** jest dostawcą tożsamości. Rejestruje użytkowników, sprawdza ich dane logowania i wystawia tokeny. Usługi AgreeOnEat nie mogą otrzymywać, haszować ani przechowywać haseł użytkowników.
- **Realm** to odizolowana przestrzeń bezpieczeństwa z własnymi użytkownikami, rolami, klientami i ustawieniami logowania. AgreeOnEat używa realmu `agreeoneat`. Specjalny realm `master` służy wyłącznie do administrowania Keycloak.
- **Klient OpenID Connect** reprezentuje aplikację rozpoczynającą logowanie. `agreeoneat-mobile` reprezentuje przyszłą aplikację React Native publikowaną w Google Play.
- **Audience** określa API, dla którego wystawiono access token. API AgreeOnEat używa wartości `agreeoneat-api`.
- **Client scope** jest zestawem ustawień tokenu i mapperów protokołu. Domyślne scope’y `basic`, `profile` i `email` dodają odpowiednio identyfikator `sub`, podstawowe dane profilu i adres e-mail. Scope `agreeoneat-api` dodaje audience API do access tokenów aplikacji mobilnej, a jawnie zdefiniowany scope `roles` mapuje role realmu do `realm_access.roles`.

## Lokalne komponenty i adresy

| Komponent | Adres lub identyfikator |
| --- | --- |
| Keycloak Admin Console | `http://localhost:8081/admin` |
| Realm | `agreeoneat` |
| OIDC Discovery | `http://localhost:8081/realms/agreeoneat/.well-known/openid-configuration` |
| Identyfikator klienta mobilnego | `agreeoneat-mobile` |
| Audience API | `agreeoneat-api` |
| Callback aplikacji mobilnej | `com.agreeoneat://oauth/callback` |
| Callback Postmana (tylko lokalne testy) | `https://oauth.pstmn.io/v1/callback` |

Dokument OIDC Discovery publikuje między innymi endpoint autoryzacji, endpoint tokenów oraz endpoint JWKS. Backend używa publicznych kluczy z JWKS do weryfikacji podpisu JWT. Prywatne klucze są generowane i przechowywane przez Keycloak i nie mogą zostać zapisane w repozytorium.

## Źródła konfiguracji

- `keycloak/realm-agreeoneat.json` jest odtwarzalną konfiguracją samego realmu: klienta, scope’ów, mapperów, ról, czasów sesji i zasad logowania.
- `AgreeOnEat-Config/user-service.yaml` jest centralną konfiguracją Spring Security Resource Server: issuer, JWKS, audience i dozwolony algorytm podpisu.
- `user-service/src/main/resources/application.yaml` zawiera tylko konfigurację startową potrzebną do odnalezienia Config Servera.
- `compose.yaml` przekazuje nadpisywalne zmienne środowiskowe dla lokalnej sieci kontenerów. `issuer-uri` pozostaje adresem widocznym w claimie `iss`, czyli `http://localhost:8081/realms/agreeoneat`, natomiast `jwk-set-uri` wskazuje z kontenera bezpośrednio na `http://keycloak:8080/.../certs`.

Rozdzielenie issuer i JWKS jest celowe: backend porównuje issuer dokładnie z wartością zapisaną w tokenie, ale publiczne klucze pobiera wewnątrz sieci Dockera. Żaden z tych adresów nie jest sekretem. Wartości można nadpisać przez `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI` i `KEYCLOAK_AUDIENCE`.

## Klient OpenID Connect aplikacji mobilnej

`agreeoneat-mobile` jest klientem publicznym, ponieważ nie można bezpiecznie ukryć współdzielonego sekretu w pliku APK. Klient używa Authorization Code Flow z PKCE:

| Ustawienie | Wartość |
| --- | --- |
| Protokół | OpenID Connect |
| Client authentication | wyłączone |
| Standard Flow | włączony |
| Metoda PKCE | `S256` |
| Direct Access Grants | wyłączone |
| Implicit Flow | wyłączony |
| Service Accounts | wyłączone |
| Device Authorization Grant | wyłączony |
| CIBA Grant | wyłączony |
| Redirect URIs | `com.agreeoneat://oauth/callback`, `https://oauth.pstmn.io/v1/callback` |
| Post-logout redirect URI | `com.agreeoneat://oauth/callback` |
| Web origins | brak |

Callback `com.agreeoneat://oauth/callback` jest własnym schematem URI aplikacji mobilnej, a nie domeną internetową. Konfiguracja deep linków i biblioteki OIDC w React Native musi używać dokładnie tego samego URI. Drugi, dokładny callback jest przeznaczony dla Postmana i pozwala zespołowi testować lokalny Authorization Code Flow bez gotowej aplikacji. Nie należy kopiować callbacku Postmana do konfiguracji środowiska produkcyjnego. Wildcardy nie mogą pozostać w wersjonowanej konfiguracji.

## Logowanie z PKCE

1. Aplikacja mobilna generuje nowy, kryptograficznie losowy `code_verifier` dla danej próby logowania.
2. Oblicza `code_challenge = BASE64URL(SHA256(code_verifier))`.
3. Otwiera systemową przeglądarkę na endpoincie autoryzacji Keycloak i przesyła challenge z metodą `S256`.
4. Keycloak wyświetla rejestrację lub logowanie i uwierzytelnia użytkownika. Aplikacja mobilna nigdy nie otrzymuje hasła.
5. Keycloak przekierowuje przeglądarkę do `com.agreeoneat://oauth/callback` z krótkotrwałym, jednorazowym authorization code.
6. Aplikacja wymienia kod i pierwotny verifier na endpoincie tokenów.
7. Keycloak porównuje verifier z wcześniejszym challenge. Nieprawidłowy lub brakujący verifier zostaje odrzucony.
8. Poprawna wymiana zwraca access token, ID token i refresh token.

PKCE chroni przechwycony authorization code. Biblioteka OIDC aplikacji mobilnej powinna również generować i weryfikować parametry `state` oraz `nonce`.

## Lokalny test w Postmanie

W kolekcji Postmana ustaw `Authorization` na `OAuth 2.0`, wyłącz `Share Token` i skonfiguruj nowy token:

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

Zaznacz `Authorize using browser` i kliknij `Get new access token`. Na stronie Keycloak zarejestruj użytkownika testowego albo zaloguj istniejącego. Jeśli przeglądarka zapyta o otwarcie Postmana, zaakceptuj; może być również konieczne zezwolenie na wyskakujące okna dla `oauth.pstmn.io`. Następnie wybierz `Proceed` i `Use Token`.

Poprawność tokenu można sprawdzić żądaniem `GET`:

```text
http://localhost:8081/realms/agreeoneat/protocol/openid-connect/userinfo
```

Żądanie ma używać odziedziczonej autoryzacji kolekcji albo nagłówka `Authorization: Bearer <access-token>`. Oczekiwany wynik to `200 OK` i co najmniej claim `sub`. Access tokenów, refresh tokenów i haseł nie należy zapisywać w kolekcji, synchronizować ani umieszczać na zrzutach ekranu.

Po uruchomieniu całego stosu ten sam access token można wysłać przez API Gateway do roboczego endpointu `user-service`:

```text
GET http://localhost:8080/api/users/me
```

Endpoint wymaga poprawnego access tokenu, ale nie sprawdza obecnie żadnej roli. Spring Security weryfikuje podpis `RS256`, issuer, czas ważności oraz audience `agreeoneat-api`, a odpowiedź pokazuje wybrane dane użytkownika i tokenu, między innymi `subject`, `clientId`, `audience`, `roles`, `algorithm` i `tokenLifetimeSeconds`. Brak tokenu albo token niepoprawny zwraca `401 Unauthorized`.

Po zmianie przypisanych client scope’ów trzeba w Postmanie pobrać nowy token przez `Get new access token` i `Use Token`. Już wystawiony JWT jest niezmienny, więc nie otrzyma nowych claimów po samej zmianie konfiguracji Keycloak.

## Tokeny i sesje

| Ustawienie | Wartość |
| --- | --- |
| Algorytm podpisu JWT | `RS256` |
| Ważność access tokenu | 20 minut |
| SSO Session Idle | 7 dni |
| SSO Session Max | 30 dni |
| Unieważnianie i rotacja refresh tokenów | włączone |
| Refresh Token Max Reuse | `0` |
| Offline access | nieprzypisany do klienta mobilnego |

Wygaśnięcie access tokenu nie wylogowuje użytkownika. Dopóki sesja SSO jest ważna, aplikacja używa refresh tokenu do uzyskania nowego access tokenu. Rotacja zwraca nowy refresh token i unieważnia poprzedni, dlatego aplikacja musi atomowo zastąpić zapisany token po każdym odświeżeniu.

Po siedmiu dniach bezczynności sesja SSO wygasa. Nawet aktywna sesja kończy się po trzydziestu dniach i wymaga ponownego interaktywnego logowania. Bezpieczne przechowywanie tokenów w aplikacji mobilnej jest osobnym zadaniem.

### Przeznaczenie tokenów

- **Access token** autoryzuje żądania do API AgreeOnEat.
- **ID token** opisuje rezultat logowania dla aplikacji mobilnej. Nie może służyć do autoryzacji żądań API.
- **Refresh token** jest wysyłany wyłącznie do endpointu tokenów Keycloak w celu uzyskania nowego zestawu tokenów.

Przykładowy payload access tokenu zawiera:

```json
{
  "iss": "http://localhost:8081/realms/agreeoneat",
  "sub": "stabilny-identyfikator-uzytkownika-keycloak",
  "azp": "agreeoneat-mobile",
  "aud": "agreeoneat-api",
  "realm_access": {
    "roles": ["USER"]
  },
  "iat": 1700000000,
  "exp": 1700001200
}
```

Rzeczywiste `iat` i `exp` są znacznikami czasu Unix; przykład pokazuje oczekiwane claimy i różnicę 1200 sekund. API musi sprawdzać podpis, issuer, czas ważności oraz obecność `agreeoneat-api` w `aud`.

## Użytkownicy i hasła

Użytkownicy rejestrują się i logują adresem e-mail. E-mail nie jest stabilnym identyfikatorem backendowym: usługi identyfikują użytkownika przez claim `sub` z JWT. Nazwa wyświetlana i pozostałe dane profilu aplikacyjnego należą do `user-service`.

| Ustawienie | Wartość |
| --- | --- |
| Samodzielna rejestracja | włączona |
| Email as username | włączone |
| Login with email | włączone |
| Duplicate emails | wyłączone, czyli duplikaty są zabronione |
| Weryfikacja e-maila | wyłączona lokalnie |
| Reset hasła przez e-mail | wyłączony lokalnie |
| Remember Me | wyłączone |

Keycloak haszuje hasła za pomocą Argon2 z rekomendowanymi parametrami domyślnymi. Polityka wymaga co najmniej 12 znaków i odrzuca hasło równe nazwie użytkownika lub adresowi e-mail. Wygasanie haseł jest wyłączone.

Przed wdrożeniem produkcyjnym należy skonfigurować SMTP, weryfikację e-maila i dostarczanie wiadomości resetujących hasło. Bez weryfikacji e-maila lokalna konfiguracja rejestracji nie jest gotowa do użycia produkcyjnego.

## Role i autoryzacja aplikacyjna

Każdy nowy użytkownik otrzymuje domyślną realm role `USER`. W przyszłym Spring Security Resource Server claim Keycloak zostanie zmapowany następująco:

```text
realm_access.roles.USER -> ROLE_USER
```

Klient `agreeoneat-mobile` ma wyłączone `Full Scope Allowed` i jawne mapowanie wyłącznie roli `USER`. Dzięki temu techniczne role Keycloak, takie jak `offline_access`, `uma_authorization` i `default-roles-agreeoneat`, nie są umieszczane w access tokenie przeznaczonym dla API AgreeOnEat.

Pozwoli to używać zabezpieczeń metod, np. `@PreAuthorize("hasRole('USER')")`. Backend powinien pobierać stabilny identyfikator użytkownika przez `Jwt#getSubject()`.

Przyszły panel administracyjny powinien otrzymać osobnego klienta OIDC, np. `agreeoneat-admin-web`, oraz osobną rolę aplikacyjną, np. `APP_ADMIN`. Administrator aplikacji nie może automatycznie otrzymywać uprawnień administracyjnych Keycloak.

Role gospodarstwa, takie jak właściciel, administrator lub członek, są kontekstowe: jedna osoba może mieć inną rolę w każdym gospodarstwie. Role te należą do `household-service`, a nie do globalnych realm roles Keycloak.

## Odtwarzalny import realmu

Docker Compose montuje `keycloak/realm-agreeoneat.json` w trybie tylko do odczytu do `/opt/keycloak/data/import/` i uruchamia Keycloak z `start-dev --import-realm`. Import tworzy realm tylko wtedy, gdy jeszcze nie istnieje. Zmiany wykonane w Admin Console są zapisane w PostgreSQL i nie aktualizują automatycznie JSON-a.

Końcowy test czystego odtworzenia celowo usunie lokalne wolumeny przed ponownym uruchomieniem Keycloak. Operacja usuwa wszystkich lokalnych użytkowników i zawartość bazy Keycloak. Prawdziwi i testowi użytkownicy, hasła, hashe haseł, dane administratora, tokeny, sekrety klientów oraz prywatne klucze nie mogą znaleźć się w wersjonowanym pliku realmu.
