# Przepływy uwierzytelniania i autoryzacji w AgreeOnEat

Póki co dokument jest tworzony po polsku aby łatwiej było mi zebrać wszystkie szczegóły. W przyszłości może zostać przetłumaczona na angielski.

## Słownik pojęć

### Realm

Realm to wydzielona przestrzeń w Keycloak przechowująca własnych użytkowników, klientów aplikacji, role, sesje oraz zasady logowania. Można go traktować jak osobne środowisko bezpieczeństwa — dane i konfiguracja jednego realmu są odseparowane od pozostałych.

AgreeOnEat korzysta z realmu `agreeoneat`. Oznacza to, że konta użytkowników aplikacji, klient `agreeoneat-mobile` i cała ustalona konfiguracja logowania należą właśnie do tej przestrzeni. Realm `master` służy natomiast do zarządzania samym Keycloak i nie powinien przechowywać zwykłych użytkowników aplikacji.

### CSRF

CSRF (Cross-Site Request Forgery) to atak polegający na nakłonieniu przeglądarki zalogowanego użytkownika do wysłania niechcianego żądania do innej strony. Jest możliwy przede wszystkim wtedy, gdy przeglądarka automatycznie dołącza do żądań dane uwierzytelniające, na przykład ciasteczko sesyjne.

Przykładowo użytkownik jest zalogowany do serwisu wykorzystującego ciasteczka, a następnie otwiera złośliwą stronę. Ta strona próbuje wysłać w jego imieniu żądanie zmieniające dane. Przeglądarka może automatycznie dołączyć ciasteczko właściwego serwisu, mimo że użytkownik świadomie nie rozpoczął tej operacji. Ochrona CSRF wymaga wtedy dodatkowej, trudnej do podrobienia wartości powiązanej z prawdziwą stroną i sesją użytkownika.

CORS i CSRF rozwiązują różne problemy: CORS określa, którym originom przeglądarka pozwala komunikować się z API, natomiast CSRF chroni serwer przed wykonaniem niechcianej operacji przy użyciu automatycznie dołączonych poświadczeń użytkownika.

### PKCE

PKCE (Proof Key for Code Exchange) to mechanizm zabezpieczający proces logowania użytkownika rozpoczęty przez aplikację, która nie może bezpiecznie przechowywać `client_secret` (takie jakby hasło do Keycloak). Aplikacja nadal wysyła publiczny `client_id` (taki jakby login), natomiast PKCE sprawia, że authorization code może wykorzystać tylko urządzenie, które rozpoczęło logowanie.

### OpenID Connect (OIDC)

OAuth 2.0 jest mechanizmem autoryzacji — odpowiada przede wszystkim na pytanie: „Do czego aplikacja może otrzymać dostęp?”. Używa do tego access tokenu, ale sam nie określa standardowego sposobu potwierdzenia, kim jest zalogowany użytkownik.

OpenID Connect jest warstwą logowania i tożsamości zbudowaną na OAuth 2.0. Odpowiada dodatkowo na pytanie: „Kim jest użytkownik?”. Dodaje między innymi ID token (opisuje wynik logowania i tożsamość użytkownika; w przeciwieństwie do access tokenu nie służy do wywoływania API), standardowe informacje o użytkowniku oraz parametr `nonce` (jednorazową losową wartość wiążącą ID token z konkretną próbą logowania).

### Scope, rola i permisja

Scope nie zastępuje modelu `rola → permisje`. Jest dodatkowym mechanizmem OAuth określającym zakres przyznany konkretnemu access tokenowi.

#### Znany model: rola jako zbiór permisji

Model powszechnie stosowany jest klasycznym RBAC (Role-Based Access Control):

```text
użytkownik
    ↓ posiada
rola
    ↓ grupuje
permisje
```

Przykładowo:

```text
ADMIN
├── READ_FLOW
├── EDIT_FLOW
├── DELETE_FLOW
├── READ_CONNECTOR
└── EDIT_CONNECTOR

READER
├── READ_FLOW
└── READ_CONNECTOR
```

Permisja jest pojedynczym uprawnieniem do wykonania operacji, na przykład `EDIT_FLOW`. Rola jest wygodną nazwą grupującą wiele permisji. Użytkownik otrzymuje rolę, a aplikacja na jej podstawie wyznacza jego permisje.

#### Czym w takim razie jest scope?

Scope należy do innej warstwy. Nie opisuje całego konta użytkownika, tylko zakres przyznany jednemu, konkretnemu access tokenowi:

```text
access token
└── scope: operacje lub dane dostępne przy użyciu tego tokenu
```

Najprościej można traktować scope jako ograniczenie możliwości konkretnego tokenu.

Przykład: użytkownik z rolą `ADMIN` posiada w systemie wszystkie permisje, ale aplikacja otrzymała token tylko ze scopem `READ_FLOW`. Jeżeli API wymaga tego scope’u, tym konkretnym tokenem można odczytywać flow, ale nie można go edytować ani usuwać. Wydanie nowego tokenu z innym zakresem nie zmienia roli użytkownika w bazie.

Gdy system jednocześnie używa ról, permisji i scope’ów, ostateczny dostęp można uprościć do:

```text
uprawnienia wynikające z roli
∩ zakres przyznany tokenowi
∩ reguły dotyczące konkretnego zasobu
= dozwolone operacje
```

Scope nie jest więc kolejną tabelą pomiędzy rolą a permisją. Jest ograniczeniem zapisanym w tokenie i sprawdzanym przez API tylko wtedy, gdy endpoint jawnie go wymaga.

#### Dlaczego scope ma czasem nazwę `email` albo `profile`?

W OAuth i OIDC słowo scope oznacza ogólnie zakres dostępu, a nie wyłącznie operację CRUD.

Scope może dotyczyć:

1. danych, które aplikacja może otrzymać;
2. operacji, które token może wykonać.

Przykłady zakresów danych OIDC:

| Scope | Co rzeczywiście oznacza? |
| --- | --- |
| `openid` | Aplikacja rozpoczyna logowanie OIDC i chce otrzymać ID token. |
| `profile` | Aplikacja może otrzymać podstawowe dane profilu, na przykład imię i nazwisko. |
| `email` | Aplikacja może otrzymać `email` i `email_verified`. |

`email` nie oznacza permisji `EDIT_EMAIL`. Oznacza wyłącznie zgodę na przekazanie aplikacji informacji o e-mailu użytkownika.

Przyszłe scope’y API mogą natomiast przypominać znane permisje:

```text
recipes.read
recipes.write
households.read
```

W takim zastosowaniu scope `recipes.read` rzeczywiście działa podobnie do permisji `READ_RECIPE`. Najważniejsza różnica polega na miejscu przypisania:

| Mechanizm | Gdzie jest przypisany? | Jak długo obowiązuje? |
| --- | --- | --- |
| Permisja wynikająca z roli | Do konta użytkownika przez rolę, zwykle w bazie lub systemie IAM. | Dopóki nie zmieni się rola albo jej permisje. |
| Scope | Do konkretnego access tokenu. | Tylko do wygaśnięcia tego tokenu. |


#### Client Scope w Keycloak to jeszcze inne pojęcie

Keycloak używa nazwy „Client Scope” również dla pakietu konfiguracji określającego zawartość tokenu. Taki Client Scope może zawierać mappery dodające claimy i nie musi być biznesową permisją.

W AgreeOnEat:

| Client Scope Keycloak | Zadanie |
| --- | --- |
| `profile` | Dodaje dane profilowe użytkownika. |
| `email` | Dodaje dane związane z e-mailem. |
| `roles` | Dodaje `realm_access.roles` do tokenu. Nie jest permisją o nazwie `roles`. |
| `agreeoneat-api` | Dodaje `aud=agreeoneat-api`. Nie daje automatycznie dostępu do wszystkich endpointów. |

Client Scope w Keycloak jest więc przede wszystkim mechanizmem konfiguracji tokenu. Tylko niektóre Client Scopes reprezentują właściwe zakresy operacji.

#### Jak widzi to Spring Security?

Po zweryfikowaniu JWT Spring zamienia role i scope’y na jedną techniczną listę `GrantedAuthority`:

```text
rola USER          → ROLE_USER
scope recipes.read → SCOPE_recipes.read
```

Endpoint wybiera, czego chce wymagać:

```java
hasRole("ADMIN")
hasAuthority("SCOPE_recipes.read")
```

`GrantedAuthority` nie jest nowym modelem permisji. Jest wspólnym formatem Spring Security, w którym może zostać zapisana zarówno rola, jak i scope.

## Przepływ frontend → mikroserwis biznesowy

![Diagram sekwencji przepływu od frontendu do mikroserwisu biznesowego](img/frontend-to-microservice-sequence.png)

### 1. Rozpoczęcie logowania

Użytkownik uruchamia aplikację mobilną i na ekranie powitalnym wybiera przycisk „Zaloguj”.

### 2. Wygenerowanie danych zabezpieczających logowanie

Aplikacja przygotowuje trzy losowe wartości potrzebne tylko podczas tej jednej próby logowania:

| Wartość | Czym jest? | Co chroni? | Najprostsze pytanie kontrolne |
| --- | --- | --- | --- |
| `state` | Losowa wartość dla jednej próby logowania. Frontend zapisuje ją i wysyła do Keycloak, a Keycloak odsyła ją bez zmian razem z authorization code. | Callback z przeglądarki. | „Czy ten callback odpowiada logowaniu, które rozpocząłem?” |
| `nonce` | Druga losowa wartość. Keycloak odsyła ją wewnątrz podpisanego ID tokenu. | ID token. | „Czy ten ID token został wystawiony dla mojego aktualnego logowania?” |
| `code_verifier` | Długi, kryptograficznie losowy sekret jednorazowy używany przez PKCE i przechowywany tymczasowo przez frontend. | Wymianę authorization code na tokeny. | „Czy authorization code wymienia ta sama aplikacja, która rozpoczęła logowanie?” |

Te wartości powstają w aplikacji mobilnej podczas działania programu. Muszą być nowe dla każdej próby logowania i nie mogą być generowane przez `Math.random()`.
W przyszłym frontendzie kod odpowiedzialny za logowanie znajdzie się w wydzielonym module, ale samo bezpieczne losowanie i sprawdzanie tych wartości powierzymy bibliotece OIDC.
Biblioteka wygeneruje `state`, `nonce` i `code_verifier`, tymczasowo przechowa je na urządzeniu oraz wykorzysta po powrocie z przeglądarki.

### 3. Wyliczenie `code_challenge`

Biblioteka OIDC wykorzystuje utworzony wcześniej `code_verifier` do wyliczenia wartości `code_challenge`:

```text
code_challenge = BASE64URL(SHA-256(code_verifier))
```

`code_verifier` pozostaje na urządzeniu i na tym etapie nie jest wysyłany. Do Keycloak zostanie wysłany jedynie `code_challenge`. SHA-256 jest funkcją jednokierunkową, dlatego na podstawie `code_challenge` nie da się w praktyce odzyskać pierwotnego `code_verifier`.

Ten wariant PKCE nazywa się `S256`. Keycloak zapamięta `code_challenge`, aby później — podczas wymiany authorization code na tokeny — sprawdzić przedstawiony przez aplikację `code_verifier`.

### 4. Otwarcie strony logowania

Biblioteka OIDC otwiera systemową przeglądarkę na endpoincie autoryzacji Keycloak. Lokalnie jest to adres:

```text
http://localhost:8081/realms/agreeoneat/protocol/openid-connect/auth
```

Używana jest systemowa przeglądarka, a nie formularz osadzony bezpośrednio w aplikacji. Dzięki temu aplikacja mobilna nie widzi i nie przechwytuje hasła użytkownika — dane logowania trafiają wyłącznie do Keycloak.

Po otwarciu przeglądarki aplikacja oczekuje na późniejszy powrót przez skonfigurowany callback. W środowisku produkcyjnym endpoint Keycloak będzie dostępny przez HTTPS.

### 5. Rozpoczęcie procesu logowania w Keycloak

Systemowa przeglądarka wysyła do endpointu autoryzacji żądanie `GET` z parametrami przygotowanymi przez bibliotekę OIDC. W uproszczeniu adres wygląda następująco:

```text
/auth
  ?response_type=code
  &client_id=agreeoneat-mobile
  &redirect_uri=com.agreeoneat://oauth/callback
  &scope=openid
  &state=<wygenerowany_state>
  &nonce=<wygenerowany_nonce>
  &code_challenge=<wyliczony_code_challenge>
  &code_challenge_method=S256
```

W tej chwili użytkownik nie podaje jeszcze e-maila ani hasła. Logowanie użytkownika nastąpi dopiero w kolejnych krokach.

| Parametr | Skąd pochodzi? | Znaczenie |
| --- | --- | --- |
| `response_type=code` | Stała konfiguracja biblioteki OIDC. | Informuje Keycloak, że po logowaniu ma zwrócić authorization code, a nie token bezpośrednio w adresie. |
| `client_id=agreeoneat-mobile` | Publiczna konfiguracja aplikacji oraz klient zarejestrowany w Keycloak. | Określa, która aplikacja rozpoczyna logowanie. |
| `redirect_uri` | Konfiguracja deep linku aplikacji i lista dozwolonych adresów klienta w Keycloak. | Określa, gdzie Keycloak ma odesłać przeglądarkę po logowaniu. Wartość musi dokładnie pasować do adresu zarejestrowanego w Keycloak. |
| `scope=openid` | Konfiguracja OIDC frontendu. | Włącza OpenID Connect, dzięki czemu oprócz tokenów OAuth 2.0 proces może zwrócić ID token opisujący logowanie użytkownika. |
| `state` | Wygenerowany w kroku 2 i tymczasowo zapisany przez frontend. | Keycloak odeśle go bez zmian w callbacku, aby frontend mógł powiązać odpowiedź z rozpoczętym logowaniem. |
| `nonce` | Wygenerowany w kroku 2 i tymczasowo zapisany przez frontend. | Keycloak umieści go w ID tokenie, aby frontend mógł później powiązać token z tą próbą logowania. |
| `code_challenge` | Wyliczony w kroku 3 z `code_verifier`. | Wiąże przyszły authorization code z jednorazowym sekretem pozostającym na urządzeniu. |
| `code_challenge_method=S256` | Konfiguracja PKCE aplikacji i klienta Keycloak. | Informuje, że `code_challenge` został wyliczony przy użyciu SHA-256. |

Na tym etapie Keycloak sprawdza między innymi, czy klient istnieje i jest włączony, czy `redirect_uri` jest dozwolony oraz czy zastosowano wymagane PKCE `S256`. `code_verifier` nadal pozostaje wyłącznie na urządzeniu i nie jest jeszcze wysyłany.

Keycloak nie porównuje `state` z wartością zapisaną przez frontend — zrobi to frontend po otrzymaniu callbacku. Podobnie właściwe sprawdzenie `nonce` nastąpi w aplikacji dopiero po otrzymaniu ID tokenu.

### 6. Wyświetlenie formularza logowania lub rejestracji

Po zaakceptowaniu parametrów żądania Keycloak wyświetla użytkownikowi własną stronę logowania. Ponieważ w realmie AgreeOnEat włączona jest samodzielna rejestracja, użytkownik może również przejść z tego miejsca do formularza tworzenia konta.

Formularz działa na stronie Keycloak otwartej w systemowej przeglądarce.  Wygląd strony będzie można później dostosować za pomocą motywu Keycloak.

Jeżeli użytkownik ma już aktywną sesję logowania w Keycloak, ten ekran może zostać pominięty i Keycloak przejdzie bezpośrednio do dalszej części procesu.

### 7. Przesłanie e-maila i hasła do Keycloak

Użytkownik wpisuje w formularzu swój adres e-mail i hasło, a systemowa przeglądarka przesyła dane bezpośrednio do Keycloak. W AgreeOnEat adres e-mail pełni funkcję loginu.

Żądanie nie przechodzi przez frontend React Native, API Gateway ani żaden mikroserwis. Dzięki temu tylko Keycloak ma dostęp do hasła użytkownika. Lokalnie komunikacja używa HTTP wyłącznie na potrzeby developmentu; w środowisku produkcyjnym formularz i dane logowania muszą być przesyłane przez HTTPS.

### 8. Weryfikacja użytkownika przez Keycloak

Keycloak wyszukuje konto w realmie `agreeoneat` na podstawie podanego adresu e-mail i sprawdza, czy konto jest aktywne. Następnie weryfikuje hasło, porównując je z jego bezpiecznie zapisaną postacią. Keycloak nie przechowuje hasła jako zwykłego tekstu — w konfiguracji AgreeOnEat do jego haszowania używany jest algorytm `Argon2`.

Jeżeli dane są nieprawidłowe albo konto jest wyłączone, Keycloak odrzuca logowanie i nie wydaje authorization code. Jeśli weryfikacja się powiedzie, Keycloak uznaje użytkownika za zalogowanego, tworzy dla niego własną sesję i przechodzi do przygotowania odpowiedzi dla frontendu. Na tym etapie tokeny nie zostały jeszcze wydane.

### 9. Przekierowanie przeglądarki z authorization code

Po udanym logowaniu Keycloak zwraca do systemowej przeglądarki odpowiedź HTTP `302 Redirect`. Informuje w niej przeglądarkę, że powinna przejść pod skonfigurowany adres callback aplikacji. Adres ma w uproszczeniu następującą postać:

```text
com.agreeoneat://oauth/callback
  ?code=<authorization_code>
  &state=<wartość_otrzymana_w_kroku_5>
```

`authorization code` jest krótkotrwałym i jednorazowym kodem potwierdzającym, że użytkownik pomyślnie przeszedł logowanie. Nie jest access tokenem i nie służy do wywoływania API. Frontend będzie mógł wymienić go na tokeny dopiero po przedstawieniu swojego `code_verifier`.

Keycloak odsyła również ten sam `state`, który otrzymał na początku procesu. Sam go nie weryfikuje — umożliwia frontendowi sprawdzenie, czy callback dotyczy rozpoczętej przez niego próby logowania.

### 10. Powrót z przeglądarki do aplikacji mobilnej

Przeglądarka wykonuje przekierowanie pod adres callback otrzymany od Keycloak:

```text
com.agreeoneat://oauth/callback
  ?code=<authorization_code>
  &state=<zwrócony_state>
```

`com.agreeoneat://` jest własnym schematem adresu, czyli deep linkiem zarejestrowanym dla aplikacji AgreeOnEat. System operacyjny telefonu rozpoznaje ten schemat, uruchamia aplikację mobilną i przekazuje jej cały adres callback wraz z parametrami `code` oraz `state`.

Biblioteka OIDC działająca we frontendzie odbiera callback i wznawia rozpoczęty wcześniej proces logowania. Aplikacja nadal nie otrzymuje e-maila ani hasła użytkownika — dostaje wyłącznie authorization code oraz `state`.

### 11. Sprawdzenie wartości `state`

Biblioteka OIDC odczytuje `state` z callbacku i porównuje go z wartością wygenerowaną oraz tymczasowo zapisaną przez frontend w kroku 2.

Jeżeli wartości są takie same, frontend wie, że callback odpowiada próbie logowania rozpoczętej przez tę aplikację. Może wtedy przejść do wymiany authorization code na tokeny.

Jeżeli `state` nie istnieje albo wartości są różne, biblioteka przerywa proces. Authorization code nie może zostać wysłany do endpointu tokenów, ponieważ callback może pochodzić z obcej lub wcześniej rozpoczętej operacji logowania.

`state` nie potwierdza jeszcze poprawności authorization code ani tożsamości użytkownika. Jego zadaniem jest powiązanie powrotu z przeglądarki z właściwą operacją logowania.

### 12. Wysłanie authorization code i `code_verifier` do Keycloak

Po poprawnym sprawdzeniu `state` biblioteka OIDC wysyła bezpośrednio z frontendu żądanie `POST` do endpointu tokenów Keycloak. Lokalnie jest to adres:

```text
http://localhost:8081/realms/agreeoneat/protocol/openid-connect/token
```

Żądanie używa formatu `application/x-www-form-urlencoded` i zawiera:

```http
POST /realms/agreeoneat/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=agreeoneat-mobile
&code=<authorization_code>
&redirect_uri=com.agreeoneat://oauth/callback
&code_verifier=<wartość_wygenerowana_w_kroku_2>
```

| Parametr | Znaczenie |
| --- | --- |
| `grant_type=authorization_code` | Informuje Keycloak, że frontend chce wymienić authorization code na tokeny. |
| `client_id=agreeoneat-mobile` | Identyfikuje klienta mobilnego, który rozpoczął logowanie. |
| `code` | Jest jednorazowym authorization code otrzymanym w callbacku. |
| `redirect_uri` | Musi być taki sam jak adres użyty podczas rozpoczęcia logowania. |
| `code_verifier` | Jest pierwotną, tajną wartością PKCE zachowaną przez frontend od kroku 2. |

Żądanie nie jest już wysyłane przez systemową przeglądarkę. Wykonuje je biblioteka OIDC działająca w aplikacji.

W tym momencie `code_verifier` po raz pierwszy trafia do Keycloak. Wartości `state` nie trzeba ponownie wysyłać, ponieważ została już sprawdzona lokalnie przez frontend.

#### Po co aż tyle kroków zamiast zwykłego logowania?

Prostszy wariant polegałby na przekazaniu e-maila i hasła do frontendu, a następnie wysłaniu ich bezpośrednio po tokeny. Oznaczałoby to jednak, że aplikacja musiałaby obsługiwać hasło użytkownika i samodzielnie brać udział w procesie uwierzytelniania. W zastosowanym przepływie hasło otrzymuje wyłącznie Keycloak, a frontend operuje jednorazowymi wartościami.

Każdy z pozornie nadmiarowych elementów chroni inny fragment procesu:

| Element | Dlaczego jest potrzebny? |
| --- | --- |
| Systemowa przeglądarka | Oddziela formularz logowania od kodu aplikacji i przekazuje hasło bezpośrednio do Keycloak. |
| `state` | Chroni przed zaakceptowaniem callbacku pochodzącego z innej operacji logowania. |
| Authorization code | Sprawia, że tokeny nie są przesyłane w adresie callback; kod jest krótkotrwały i jednorazowy. |
| `code_verifier` i PKCE | Powodują, że przechwycony authorization code jest bezużyteczny bez sekretu pozostałego na urządzeniu. |
| `nonce` | Pozwala później sprawdzić, czy ID token należy do tej konkretnej próby logowania. |

Dla użytkownika nadal wygląda to jak zwykłe logowanie: wybiera przycisk, podaje dane i wraca do aplikacji. Większość opisanych kroków wykonuje automatycznie biblioteka OIDC. HTTPS zabezpiecza samą transmisję, natomiast opisane mechanizmy chronią również przed użyciem przechwyconych lub podstawionych elementów procesu.

### 13. Weryfikacja authorization code i PKCE przez Keycloak

Po otrzymaniu żądania do endpointu `/token` Keycloak sprawdza, czy wszystkie jego elementy należą do tej samej, prawidłowo rozpoczętej operacji logowania.

Keycloak kontroluje między innymi:

| Kontrola | Co jest sprawdzane? |
| --- | --- |
| Authorization code | Czy kod został wystawiony przez Keycloak, nie wygasł i nie został wcześniej wykorzystany. |
| Klient | Czy kod został wystawiony dla klienta `agreeoneat-mobile`, który teraz próbuje go wymienić. |
| `redirect_uri` | Czy jest dokładnie taki sam jak adres przesłany podczas rozpoczęcia logowania. |
| PKCE | Czy otrzymany `code_verifier` odpowiada `code_challenge` zapisanemu przez Keycloak w kroku 5. |

Weryfikacja PKCE polega na ponownym wykonaniu przez Keycloak tego samego obliczenia, które wcześniej wykonał frontend:

```text
BASE64URL(SHA-256(otrzymany_code_verifier))
    ==
code_challenge zapisany przy rozpoczęciu logowania
```

Frontend nie przesyła więc wcześniej utworzonego `code_challenge` po raz drugi. Keycloak sam wylicza go z otrzymanego `code_verifier` i porównuje z wartością zapamiętaną przy wydawaniu authorization code. Zgodność stanowi dowód, że kod wymienia ta sama aplikacja, która rozpoczęła logowanie i zachowała jednorazowy sekret.

Jeżeli którakolwiek kontrola się nie powiedzie, Keycloak odrzuca żądanie i nie wydaje tokenów. Po prawidłowej wymianie authorization code zostaje zużyty, dlatego nie można wykorzystać go ponownie. Jeśli wszystkie dane są poprawne, Keycloak może przejść do utworzenia i podpisania tokenów.

### 14. Utworzenie i zwrócenie tokenów do frontendu

Po pomyślnej weryfikacji Keycloak zwraca bibliotece OIDC odpowiedź zawierającą trzy różne tokeny. W uproszczeniu wygląda ona następująco:

```json
{
  "access_token": "<access_token>",
  "id_token": "<id_token>",
  "refresh_token": "<refresh_token>",
  "token_type": "Bearer",
  "expires_in": 1200
}
```

Każdy token ma inne przeznaczenie:

| Token | Do czego służy? |
| --- | --- |
| Access token | Jest poświadczeniem dostępu do backendu. Frontend będzie dołączał go jako `Authorization: Bearer <access_token>` do żądań wysyłanych przez API Gateway. W konfiguracji AgreeOnEat jest ważny przez 1200 sekund, czyli 20 minut. |
| ID token | Potwierdza wynik logowania i opisuje tożsamość użytkownika. Zawiera między innymi `sub` oraz `nonce`. Jest przeznaczony dla frontendu i nie służy do wywoływania API. |
| Refresh token | Pozwala bibliotece OIDC uzyskać nowe tokeny bez ponownego wpisywania hasła. Jest szczególnie wrażliwy, ponieważ umożliwia przedłużanie zalogowanej sesji. |

Access token i ID token są tokenami JWT podpisanymi przez Keycloak algorytmem `RS256`. Keycloak używa do podpisu prywatnego klucza RSA należącego do realmu `agreeoneat`, a odpowiadający mu klucz publiczny udostępnia do weryfikacji. Podpis zapewnia autentyczność i integralność tokenu, ale nie szyfruje jego zawartości — danych zapisanych w JWT nie należy traktować jako tajnych.

Po otrzymaniu odpowiedzi biblioteka OIDC sprawdza ID token, w tym jego podpis, wystawcę, odbiorcę, termin ważności oraz `nonce`. Otrzymany `nonce` musi być równy wartości zapisanej przez frontend w kroku 2. Niezgodność oznacza, że ID token nie należy do bieżącej operacji logowania i cała odpowiedź musi zostać odrzucona.

Refresh token powinien być przechowywany w bezpiecznym magazynie systemowym urządzenia, na przykład Android Keystore. Nie powinien trafiać do zwykłego `AsyncStorage` ani do logów. Aktualna konfiguracja pozwala utrzymać sesję przez 7 dni bezczynności, jednak nie dłużej niż 30 dni łącznie. Przy odnowieniu Keycloak wydaje nowy refresh token, a poprzedni przestaje być ważny.

Po zakończeniu tych kontroli frontend może uznać użytkownika za zalogowanego. Od tej chwili do komunikacji z backendem będzie używał access tokenu, a nie ID tokenu ani refresh tokenu.

### 15. Wysłanie żądania do API Gateway z access tokenem

Gdy zalogowany użytkownik wykonuje operację wymagającą danych z backendu, frontend wysyła żądanie do API Gateway i umieszcza access token w nagłówku `Authorization`. Przykładowe lokalne wywołanie informacji o aktualnym użytkowniku wygląda następująco:

```http
GET http://localhost:8080/api/users/me
Authorization: Bearer <access_token>
```

Frontend używa tutaj wyłącznie access tokenu.

Słowo `Bearer` oznacza, że dostęp otrzymuje posiadacz poprawnego tokenu. Z tego powodu token nie może trafiać do adresu URL, logów ani komunikacji bez szyfrowania. W środowisku produkcyjnym żądania do Gateway muszą korzystać z HTTPS.

API Gateway działający lokalnie na porcie `8080` jest punktem wejścia do biznesowej części backendu. Frontend nie wybiera konkretnej instancji mikroserwisu i nie wysyła do niej żądania bezpośrednio. Użyta ścieżka, na przykład `/api/users/**`, pozwoli Gateway później rozpoznać usługę docelową.

Samo otrzymanie nagłówka nie oznacza jeszcze, że Gateway ufa tokenowi. Przed przekazaniem żądania dalej musi zweryfikować podpis JWT, jego wystawcę, odbiorcę, termin ważności oraz reguły dostępu. 

Natywna aplikacja mobilna i Postman nie podlegają mechanizmowi CORS przeglądarki. Jeżeli powstanie frontend przeglądarkowy działający lokalnie na `http://localhost:3000`, przeglądarka przed właściwym żądaniem może automatycznie wysłać do Gateway zapytanie `OPTIONS`, a konfiguracja CORS zdecyduje, czy ten origin i nagłówek `Authorization` są dozwolone.

### 16. Pobranie kluczy publicznych Keycloak przez API Gateway

Access token zawiera w swoim nagłówku pole `kid` (Key ID), które wskazuje identyfikator klucza użytego przez Keycloak do podpisania JWT. Gateway może odczytać nagłówek tokenu przed jego zweryfikowaniem, ale na tym etapie jeszcze mu nie ufa — wykorzystuje `kid` jedynie do odnalezienia odpowiedniego klucza publicznego.

Spring Security przechowuje pobrane klucze w pamięci. Jeżeli klucza o podanym `kid` jeszcze tam nie ma, na przykład podczas pierwszego żądania albo po rotacji kluczy w Keycloak, Gateway wysyła żądanie `GET` do endpointu JWKS realmu:

```http
GET /realms/agreeoneat/protocol/openid-connect/certs
```

Pełny adres pochodzi z właściwości:

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri
```

W zależności od sposobu uruchomienia ma on inną postać:

| Środowisko | Adres JWKS |
| --- | --- |
| Gateway uruchomiony lokalnie poza Dockerem | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs` |
| Gateway uruchomiony w Dockerze | `http://keycloak:8080/realms/agreeoneat/protocol/openid-connect/certs` |

Różnica wynika z sieci kontenerów: dla Gateway działającego w Dockerze nazwa `keycloak` wskazuje kontener Keycloak, a jego wewnętrzny port to `8080`. Port `8081` jest mapowaniem używanym przez programy działające na komputerze gospodarza.

Gateway nie wykonuje tego żądania przy każdym wywołaniu API. Jeśli potrzebny klucz znajduje się już w pamięci, kroki 16 i 17 zostają pominięte. Pobieraniem i przechowywaniem kluczy zarządza automatycznie Spring Security.

### 17. Zwrócenie zestawu kluczy JWKS przez Keycloak

Keycloak odpowiada dokumentem JWKS (JSON Web Key Set), czyli zestawem publicznych kluczy, którymi można sprawdzać podpisy tokenów wystawionych przez realm `agreeoneat`. Uproszczona odpowiedź wygląda następująco:

```json
{
  "keys": [
    {
      "kid": "<identyfikator_klucza>",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "n": "<modulus_RSA>",
      "e": "AQAB"
    }
  ]
}
```

Najważniejsze pola odpowiedzi oznaczają:

| Pole | Znaczenie |
| --- | --- |
| `kid` | Identyfikator klucza. Spring Security wybiera klucz pasujący do `kid` odczytanego z nagłówka access tokenu. |
| `kty=RSA` | Informuje, że jest to klucz RSA. |
| `use=sig` | Określa, że klucz służy do weryfikowania podpisów. |
| `alg=RS256` | Wskazuje algorytm podpisu powiązany z kluczem. |
| `n` i `e` | Publiczne parametry RSA, na podstawie których Spring Security tworzy klucz publiczny. |

Odpowiedź może zawierać kilka kluczy, między innymi podczas ich rotacji. Dzięki polu `kid` Gateway wybiera dokładnie ten, którym podpisano otrzymany token. Wybrany klucz zostaje zapisany w pamięci i może być używany przy kolejnych żądaniach.

JWKS zawiera wyłącznie dane publiczne. Nie udostępnia prywatnego klucza Keycloak, dlatego pobranego klucza nie można użyć do utworzenia poprawnie podpisanego tokenu. Endpoint może być publicznie dostępny — jego zadaniem jest umożliwienie odbiorcom niezależnego sprawdzania podpisów.

### 18. Weryfikacja access tokenu przez API Gateway

Po znalezieniu właściwego klucza publicznego Spring Security rozpoczyna lokalną weryfikację access tokenu. JWT składa się z trzech części:

- `header` zawiera informacje techniczne, między innymi `alg` i `kid`;
- `payload` zawiera claimy, czyli dane oraz ograniczenia tokenu;
- `signature` jest podpisem utworzonym przez Keycloak.

Samo odczytanie `header` i `payload` nie dowodzi, że token jest prawdziwy, ponieważ są one jedynie zakodowane za pomocą Base64URL. Dopiero poprawna weryfikacja podpisu i claimów pozwala Gateway zaufać tokenowi.

Aktualna konfiguracja wymaga następujących kontroli:

| Kontrola | Oczekiwana wartość | Znaczenie |
| --- | --- | --- |
| Podpis | Poprawny dla danych JWT i klucza publicznego wybranego przez `kid`. | Potwierdza, że token podpisał posiadacz prywatnego klucza realmu i że zawartość nie została zmieniona. |
| `alg` | `RS256` | Gateway nie akceptuje tokenu deklarującego inny algorytm. `RS256` oznacza podpis RSA wykorzystujący SHA-256. |
| `iss` | `http://localhost:8081/realms/agreeoneat` w środowisku lokalnym | Potwierdza, że token został wystawiony przez oczekiwany realm Keycloak. W produkcji będzie to publiczny adres produkcyjnego Keycloak. |
| `aud` | Musi zawierać `agreeoneat-api`. | Potwierdza, że token został przeznaczony dla backendu AgreeOnEat, a nie dla innego systemu. |
| `exp` | Termin ważności musi znajdować się w przyszłości. | Powoduje odrzucenie wygasłego tokenu. |

Wartości konfiguracyjne pochodzą z ustawień Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8081/realms/agreeoneat}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs}
          audiences: ${KEYCLOAK_AUDIENCE:agreeoneat-api}
          jws-algorithms: RS256
```

`issuer-uri` jest oczekiwaną tożsamością wystawcy i musi dokładnie odpowiadać claimowi `iss`. `jwk-set-uri` jest natomiast technicznym adresem pobierania kluczy, dlatego wewnątrz Dockera może wskazywać `keycloak:8080`, mimo że `iss` nadal zawiera publiczny adres `localhost:8081`.

Jeżeli którakolwiek kontrola zakończy się niepowodzeniem, Spring Security przerywa obsługę żądania. `RestSecurityErrorHandler` zwraca `401 Unauthorized`, a żądanie nie trafia do mikroserwisu.

Jeżeli token jest poprawny, Spring Security tworzy uwierzytelnienie użytkownika dla bieżącego żądania. Gateway działa bezstanowo (`STATELESS`), więc nie tworzy własnej sesji logowania i wykonuje tę weryfikację przy każdym następnym żądaniu z access tokenem. Poprawny token nie oznacza jeszcze automatycznej zgody na każdą ścieżkę.

### 19. Sprawdzenie reguł dostępu do ścieżki

Po uwierzytelnieniu użytkownika Spring Security sprawdza, czy żądana ścieżka jest dozwolona przez `SecurityFilterChain` w API Gateway. Jest to etap autoryzacji żądania: krok 18 odpowiedział na pytanie „czy token jest poprawny?”, a krok 19 odpowiada na pytanie „czy z takim uwierzytelnieniem wolno wejść pod ten adres?”.

Aktualne reguły są zdefiniowane w następującej kolejności:

```java
.authorizeHttpRequests(authorize -> authorize
        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().denyAll())
```

| Reguła | Znaczenie |
| --- | --- |
| `DispatcherType.ERROR` → `permitAll()` | Pozwala aplikacji wewnętrznie obsłużyć i sformatować powstały błąd. Nie udostępnia dodatkowego endpointu biznesowego. |
| `/actuator/health`, `/actuator/health/**`, `/actuator/info` → `permitAll()` | Pozwala narzędziom infrastruktury sprawdzić stan Gateway bez tokenu. |
| `OPTIONS /api/**` → `permitAll()` | Pozwala przeglądarce wykonać zapytanie wstępne CORS. Żądanie `OPTIONS` nie zwraca danych biznesowych. |
| `/api/**` → `authenticated()` | Każde właściwe żądanie do API wymaga użytkownika uwierzytelnionego poprawnym access tokenem. |
| Każda pozostała ścieżka → `denyAll()` | Wszystko, czego jawnie nie dopuszczono, jest domyślnie blokowane. |

Przykładowe żądanie `GET /api/users/me` pasuje do reguły `/api/**`. Ponieważ access token został pomyślnie zweryfikowany w kroku 18, warunek `authenticated()` jest spełniony i żądanie może przejść dalej do mechanizmu routingu Gateway.

Gateway nie sprawdza obecnie w tym miejscu roli `USER` ani szczegółowych uprawnień domenowych. Reguła `authenticated()` wymaga tylko poprawnie uwierzytelnionego użytkownika. Bardziej szczegółowe decyzje, na przykład czy użytkownik należy do danego pokoju, muszą zostać podjęte przez właściwy mikroserwis na podstawie jego danych biznesowych.

Brak lub niepoprawny token przy chronionym `/api/**` kończy się odpowiedzią `401 Unauthorized`. Poprawnie uwierzytelniony użytkownik próbujący wejść na niedozwoloną ścieżkę otrzyma `403 Forbidden`. W obu przypadkach żądanie nie zostanie przekazane do mikroserwisu.

Gateway ma również wyłączoną ochronę CSRF, ponieważ korzysta z bezstanowego uwierzytelniania tokenem Bearer w nagłówku, a nie z automatycznie dołączanego ciasteczka sesyjnego.

### 20. Odnalezienie usługi docelowej przez API Gateway

Po przejściu kontroli bezpieczeństwa Gateway dopasowuje ścieżkę żądania do jednej ze skonfigurowanych tras. Aktualne mapowanie wygląda następująco:

```yaml
routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/users/**
  - id: household-service
    uri: lb://household-service
    predicates:
      - Path=/api/households/**
  - id: recipe-service
    uri: lb://recipe-service
    predicates:
      - Path=/api/recipes/**
  - id: meal-planning-service
    uri: lb://meal-planning-service
    predicates:
      - Path=/api/meal-plans/**
```

Dla przykładowego żądania `GET /api/users/me` pasuje predykat `Path=/api/users/**`, dlatego Gateway wybiera trasę o identyfikatorze `user-service` i logicznym adresie:

```text
lb://user-service
```

`lb` oznacza Spring Cloud LoadBalancer. Adres nie wskazuje konkretnego hosta ani portu. `user-service` jest nazwą usługi, pod którą jej działające instancje rejestrują się w Eureka. Dzięki temu Gateway nie musi mieć na stałe wpisanego adresu w rodzaju `http://user-service:8082`.

Każdy mikroserwis podaje swoją nazwę w konfiguracji aplikacji, na przykład:

```yaml
spring:
  application:
    name: user-service
```

Po uruchomieniu `user-service` zgłasza do Eureka swoją nazwę, adres oraz port. Gateway korzysta z klienta Eureka i Spring Cloud LoadBalancer, aby uzyskać listę dostępnych instancji o nazwie `user-service`. Jeśli działa kilka instancji, LoadBalancer może wybrać jedną z nich i rozdzielać między nie kolejne żądania. Przy jednej instancji wybór jest oczywisty, ale ten sam mechanizm pozwala później skalować usługę.

Strzałka Gateway → Eureka na diagramie przedstawia logiczne wyszukanie usługi. W praktyce klient Eureka okresowo pobiera rejestr i przechowuje jego lokalną kopię, dlatego Gateway zwykle nie wykonuje osobnego zapytania HTTP do Eureka przy każdym żądaniu użytkownika.

 Jeśli w rejestrze nie ma żadnej dostępnej instancji `user-service`, Gateway nie ma dokąd przekazać żądania i powinien zakończyć je błędem `503 Service Unavailable`.

W konfiguracji tras nie ma filtra usuwającego fragment ścieżki, dlatego oryginalne `/api/users/me` pozostaje bez zmian.

### 21. Zwrócenie adresu instancji mikroserwisu

Eureka udostępnia Gateway informacje o instancjach zarejestrowanych pod nazwą `user-service`. W uproszczeniu wpis takiej instancji zawiera:

```text
serviceId: user-service
status: UP
host: <wewnętrzny adres instancji>
port: 8082
```

`serviceId` musi odpowiadać nazwie użytej w `lb://user-service`. Status `UP` informuje, że instancja jest zarejestrowana jako dostępna, natomiast `host` i `port` pozwalają nawiązać z nią połączenie.

W obecnej konfiguracji mikroserwisy używają:

```yaml
eureka:
  instance:
    prefer-ip-address: true
```

Oznacza to, że instancja preferuje zarejestrowanie swojego adresu IP zamiast nazwy hosta. W Dockerze jest to adres osiągalny wewnątrz sieci kontenerów. Nie wpisujemy go na stałe do konfiguracji Gateway, ponieważ może zmienić się po ponownym uruchomieniu kontenera.

Jeżeli Eureka zna kilka instancji `user-service`, Spring Cloud LoadBalancer wybiera jedną z nich. Wynikiem tego kroku jest rozwiązanie logicznego adresu:

```text
lb://user-service
```

do rzeczywistego adresu podobnego do:

```text
http://<wewnętrzny-adres-instancji>:8082
```

Na diagramie Eureka odsyła te dane bezpośrednio do Gateway. W działającej aplikacji mogą one pochodzić z lokalnej kopii rejestru okresowo synchronizowanej przez klienta Eureka, ale rezultat jest ten sam: Gateway otrzymuje adres wybranej instancji.

### 22. Przekazanie żądania i access tokenu do mikroserwisu

Po wybraniu instancji Gateway działa jak reverse proxy: tworzy połączenie z wewnętrznym adresem `user-service` i przekazuje do niego żądanie użytkownika.
Gateway zmienia adres sieciowy odbiorcy, ale zachowuje elementy potrzebne mikroserwisowi do obsłużenia operacji:

| Element | Zachowanie Gateway |
| --- | --- |
| Metoda HTTP | Pozostaje bez zmian, na przykład `GET`. |
| Ścieżka | Pozostaje `/api/users/me`, ponieważ nie skonfigurowano filtra przepisującego lub skracającego ścieżkę. |
| Parametry zapytania | Są przekazywane dalej. |
| Body | Dla metod posiadających treść, na przykład `POST`, jest przesyłane do mikroserwisu. |
| Nagłówek `Authorization` | Jest przekazywany z tym samym access tokenem użytkownika. |

Mikroserwis otrzymuje oryginalny, kryptograficznie podpisany access token Keycloak. Po jego zweryfikowaniu będzie mógł samodzielnie odczytać między innymi `sub`, scope’y i role.

Ponowna weryfikacja w mikroserwisie jest celowa. Gateway chroni wejście do systemu, ale mikroserwis nie powinien zakładać, że każde żądanie z sieci wewnętrznej na pewno przeszło przez Gateway. Żądanie może pochodzić z innego kontenera, błędnie skonfigurowanej trasy albo w przyszłości z komunikacji S2S. Mikroserwis stanowi więc drugą granicę bezpieczeństwa i sam sprawdza poświadczenie, na podstawie którego wykonuje logikę biznesową.

W aktualnym Docker Compose port `user-service` nie jest publikowany na komputerze gospodarza, podczas gdy port `8080` API Gateway jest publicznie mapowany. Ogranicza to możliwość ominięcia Gateway z zewnątrz, ale inne elementy wewnętrznej sieci kontenerów nadal mogą dotrzeć do mikroserwisu. Ochrona sieciowa i weryfikacja JWT uzupełniają się, zamiast wzajemnie zastępować.

### 23. Pobranie kluczy publicznych Keycloak przez `user-service`

Po odebraniu żądania Spring Security w `user-service` odczytuje z nagłówka access tokenu identyfikator `kid`. Podobnie jak wcześniej Gateway, mikroserwis potrzebuje odpowiadającego mu klucza publicznego, zanim będzie mógł zaufać podpisowi JWT.

Gateway i `user-service` są osobnymi procesami i mają osobne pamięci. Klucz pobrany wcześniej przez Gateway nie jest automatycznie przekazywany do mikroserwisu. Każda instancja usługi samodzielnie utrzymuje własny cache kluczy i wykonuje własną weryfikację kryptograficzną.

Jeżeli `user-service` nie ma w pamięci klucza odpowiadającego `kid`, na przykład obsługuje pierwszy token po uruchomieniu albo Keycloak wykonał rotację kluczy, wysyła żądanie:

```http
GET /realms/agreeoneat/protocol/openid-connect/certs
```

Adres pochodzi z tej samej właściwości Resource Server co w Gateway:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs}
```

W zależności od miejsca uruchomienia używany jest odpowiedni adres:

| Środowisko | Adres JWKS używany przez `user-service` |
| --- | --- |
| Usługa uruchomiona lokalnie poza Dockerem | `http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs` |
| Usługa uruchomiona w Dockerze | `http://keycloak:8080/realms/agreeoneat/protocol/openid-connect/certs` |

`user-service` nie wysyła do Keycloak access tokenu użytkownika z pytaniem, czy jest on poprawny. Nie korzysta tutaj z endpointu introspekcji. Pobiera jedynie publiczne klucze i później samodzielnie sprawdzi token lokalnie. Dzięki temu Keycloak nie musi uczestniczyć w obsłudze każdego żądania biznesowego.

Krok jest warunkowy. Jeśli pasujący klucz znajduje się już w cache danej instancji `user-service`, kroki 23 i 24 zostają pominięte. Przy wielu instancjach każda z nich posiada własny cache i w razie potrzeby pobiera JWKS niezależnie.

### 24. Zwrócenie zestawu JWKS do `user-service`

Keycloak odpowiada do `user-service` zestawem publicznych kluczy realmu `agreeoneat`. Jest to ten sam dokument JWKS, który wcześniej mógł pobrać API Gateway:

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "keys": [
    {
      "kid": "<identyfikator_klucza>",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "n": "<modulus_RSA>",
      "e": "AQAB"
    }
  ]
}
```

Spring Security porównuje `kid` z nagłówka access tokenu z identyfikatorami kluczy znajdujących się w tablicy `keys`. Następnie wybiera pasujący klucz RSA przeznaczony do weryfikowania podpisów i tworzy z jego publicznych parametrów `n` oraz `e` obiekt klucza publicznego.

JWKS może zawierać kilka kluczy, ponieważ Keycloak musi obsługiwać ich rotację. Nowe tokeny mogą być podpisywane nowym kluczem, podczas gdy starszy klucz publiczny pozostaje jeszcze dostępny do czasu wygaśnięcia wcześniej wydanych tokenów. Pole `kid` pozwala wybrać właściwy klucz bez prób sprawdzania podpisu każdym z nich.

Wybrany zestaw kluczy zostaje zachowany w cache konkretnej instancji `user-service`. Nie jest współdzielony z Gateway ani z innymi instancjami mikroserwisu.

Odpowiedź nie zawiera prywatnego klucza Keycloak. Posiadanie JWKS pozwala jedynie weryfikować istniejące podpisy, a nie wystawiać nowe, poprawnie podpisane tokeny.

Jeżeli po odświeżeniu JWKS nadal nie istnieje klucz odpowiadający `kid` tokenu albo zestawu nie uda się pobrać, `user-service` nie może bezpiecznie potwierdzić podpisu. Token musi wtedy zostać odrzucony, a żądanie nie może dotrzeć do kontrolera.

### 25. Ponowna weryfikacja access tokenu przez `user-service`

Filtry Spring Security przechwytują żądanie, zanim trafi ono do kontrolera `user-service`. Mikroserwis traktuje otrzymany JWT jako niezaufany i samodzielnie wykonuje te same podstawowe kontrole kryptograficzne oraz czasowe co API Gateway.

Konfiguracja Resource Server w `user-service` zawiera:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8081/realms/agreeoneat}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8081/realms/agreeoneat/protocol/openid-connect/certs}
          audiences: ${KEYCLOAK_AUDIENCE:agreeoneat-api}
          jws-algorithms: RS256
```

Na jej podstawie Spring Security sprawdza:

| Kontrola | Wymaganie |
| --- | --- |
| Podpis | Musi pasować do `header.payload` tokenu i publicznego klucza RSA wybranego na podstawie `kid`. |
| `alg` | Dozwolony jest wyłącznie `RS256`. |
| `iss` | Musi dokładnie odpowiadać skonfigurowanemu adresowi realmu `agreeoneat`. |
| `aud` | Musi zawierać `agreeoneat-api`, ponieważ token ma być przeznaczony dla backendu AgreeOnEat. |
| `exp` | Token nie może być przeterminowany. |

Weryfikacja podpisu odbywa się lokalnie przy użyciu klucza publicznego. `user-service` nie przesyła tokenu do Keycloak ani do Gateway i nie potrzebuje klucza prywatnego. Jeśli odpowiedni klucz jest już w cache, sprawdzenie nie wymaga żadnego dodatkowego połączenia sieciowego.

Jeżeli którakolwiek kontrola zakończy się niepowodzeniem, Spring Security nie uruchamia kontrolera. `RestSecurityErrorHandler` zwraca:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "A valid Bearer access token is required."
}
```

Jeżeli token przejdzie wszystkie kontrole, Spring Security tworzy dla bieżącego żądania obiekt uwierzytelnienia zawierający zweryfikowany JWT. `user-service` działa bezstanowo, więc nie tworzy własnej sesji i powtarza walidację przy każdym żądaniu.

Lokalna walidacja ma również ważną konsekwencję: mikroserwis ocenia podpis i claimy zapisane w tokenie, ale nie pyta Keycloak o aktualny stan konta przy każdym wywołaniu. Jeżeli użytkownik wyloguje się albo administrator wyłączy konto, wcześniej wydany access token może pozostać akceptowany do czasu swojego wygaśnięcia — w AgreeOnEat maksymalnie przez 20 minut. Refresh token nie pozwoli wtedy bez końca przedłużać dostępu, ale krótki czas życia access tokenu ogranicza okno użycia już wydanego JWT.

Poprawna walidacja oznacza dopiero, że token jest autentyczny, przeznaczony dla tego backendu i nadal ważny. Scope’y oraz role zawarte w tokenie zostaną przekształcone na uprawnienia Spring Security w kroku 26.

### 26. Mapowanie scope’ów i ról na uprawnienia Spring Security

Po zweryfikowaniu JWT `user-service` odczytuje z niego informacje, które mają reprezentować uprawnienia użytkownika. W tokenie Keycloak są one zapisane jako zwykłe claimy, na przykład:

```json
{
  "scope": "openid profile email",
  "realm_access": {
    "roles": ["USER"]
  }
}
```

Spring Security nie podejmuje decyzji bezpośrednio na podstawie takiego JSON-a. Najpierw zamienia wartości na obiekty `GrantedAuthority`, czyli ujednolicone nazwy uprawnień wykorzystywane później przez reguły bezpieczeństwa.

#### Mapowanie scope’ów

Standardowy `JwtGrantedAuthoritiesConverter` odczytuje claim `scope` i dodaje do każdej wartości prefiks `SCOPE_`:

```text
openid  → SCOPE_openid
profile → SCOPE_profile
email   → SCOPE_email
```

Scope opisuje zakres przyznany tokenowi lub klientowi. Nie każdy scope OIDC jest jednak uprawnieniem biznesowym. Przykładowo `profile` i `email` określają przede wszystkim zestawy informacji o użytkowniku.

#### Mapowanie ról Keycloak

Role realmu znajdują się w zagnieżdżonym claimie `realm_access.roles`, którego standardowy converter Springa nie obsługuje. Dlatego `user-service` posiada dwie własne klasy:

1. `KeycloakRealmRolesExtractor` bezpiecznie odczytuje tablicę ról z `realm_access.roles`. Jeżeli claimu nie ma albo ma niewłaściwy format, zwraca pustą listę.
2. `KeycloakRealmRoleConverter` zamienia każdą odczytaną rolę na `GrantedAuthority` z prefiksem `ROLE_`.

Przykładowe mapowanie wygląda następująco:

```text
USER       → ROLE_USER
ADMIN      → ROLE_ADMIN
ROLE_ADMIN → ROLE_ADMIN
```

Converter nie dodaje prefiksu drugi raz, jeżeli nazwa roli już zaczyna się od `ROLE_`. Nazwy są rozróżniane pod względem wielkości liter, dlatego role AgreeOnEat zapisujemy w spójnej postaci, na przykład `USER`.

#### Połączenie obu źródeł

Bean `JwtAuthenticationConverter` łączy converter scope’ów i converter ról. Dla przykładowego tokenu wynikiem będzie uwierzytelnienie zawierające między innymi:

```text
SCOPE_openid
SCOPE_profile
SCOPE_email
ROLE_USER
```

Dzięki temu dalsze reguły mogą być zapisywane w standardowy sposób Spring Security:

```java
hasRole("USER")
hasAuthority("SCOPE_recipes.read")
```

`hasRole("USER")` automatycznie szuka authority `ROLE_USER`, natomiast dla scope’u podaje się pełną nazwę `SCOPE_...`.