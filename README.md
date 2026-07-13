# AgreeOnEat

AgreeOnEat pomaga domownikom wspólnie wybrać posiłek. Obecnie repozytorium zawiera uruchamialny szkielet mikroserwisów: konfigurację, discovery, gateway, Keycloak oraz endpointy kontrolne `Hello`.

## Architektura

| Komponent | Odpowiedzialność |
| --- | --- |
| `config-server` | Pobiera konfigurację usług z osobnego repozytorium Git [`AgreeOnEat-Config`](https://github.com/MateuszKosowski/AgreeOnEat-Config). |
| `discovery-server` | Eureka — rejestr działających instancji usług. |
| `api-gateway` | Jedyny publiczny punkt wejścia do API; przekazuje żądania do usług przez Eurekę. |
| `user-service` | Docelowo profil użytkownika: nazwa, alergeny, nielubiane składniki i ustawienia. Konta, hasła oraz tokeny należą do Keycloaka. |
| `household-service` | Docelowo pokoje/domostwa, zaproszenia, role i członkostwa. |
| `recipe-service` | Docelowo katalog przepisów, składniki, alergeny, makro, cena i czas przygotowania. |
| `meal-planning-service` | Docelowo sesje wyboru posiłku, swipy, matche, eliminacja i plan tygodniowy. |
| `keycloak` | Rejestracja, logowanie, JWT i w przyszłości Google OAuth. |
| `keycloak-db` | PostgreSQL używany wyłącznie przez Keycloak. |

## Wymagania

- Docker Desktop z uruchomionym silnikiem Docker;

## Uruchomienie lokalne

### 1. Uruchom Docker Desktop

Poczekaj, aż Docker Desktop uruchomi silnik Docker.

### 2. Wygeneruj token GitHub

Config Server pobiera konfigurację z prywatnego repozytorium `AgreeOnEat-Config`, dlatego potrzebuje tokenu GitHub tylko do odczytu:

1. W GitHub kliknij avatar w prawym górnym rogu, a następnie przejdź do `Settings` -> `Developer settings` -> `Personal access tokens` -> `Fine-grained tokens`.
2. Wybierz `Generate new token`.
3. W polu **Resource owner** wybierz swoje konto, a w polu **Repository access** wybierz `Only select repositories` i wskaż wyłącznie `AgreeOnEat-Config`.
4. W sekcji **Repository permissions** ustaw `Contents` na `Read-only`.
5. Wygeneruj token i od razu skopiuj jego wartość — GitHub pokaże ją tylko raz.

### 3. Przygotuj plik `.env`

W PowerShell, w katalogu głównym projektu:

```powershell
Copy-Item .env.example .env
```

W `.env` podmień wartości na własne lokalne dane:

```text
KEYCLOAK_ADMIN_PASSWORD=twoje-lokalne-haslo
KEYCLOAK_DB_PASSWORD=inne-lokalne-haslo
CONFIG_REPOSITORY_USERNAME=MateuszKosowski
CONFIG_REPOSITORY_TOKEN=github_pat_...
```

### 4. Uruchom i sprawdź usługi

```powershell
docker compose up -d --build
```

Jeżeli usługa wystartowała przed Config Serverem, Compose uruchomi ją ponownie. Po kilkunastu sekundach wszystkie usługi powinny pojawić się w Eurece.

## Adresy lokalne

| Adres | Przeznaczenie |
| --- | --- |
| [http://localhost:8080](http://localhost:8080) | API Gateway |
| [http://localhost:8081](http://localhost:8081) | panel Keycloak |
| [http://localhost:8761](http://localhost:8761) | panel Eureki |
| [http://localhost:8888](http://localhost:8888) | Config Server |

Panel Keycloak używa danych `KEYCLOAK_ADMIN` i `KEYCLOAK_ADMIN_PASSWORD` z pliku `.env`. Realm `agreeoneat` i klient mobilny `agreeoneat-mobile` są importowane przy pierwszym starcie.

## Zatrzymanie i reset

```powershell
docker compose down
```

Powyższe polecenie zachowuje wolumen PostgreSQL Keycloaka. Pełny reset danych lokalnych (w tym użytkowników Keycloaka) wykonasz przez:

```powershell
docker compose down -v
```

## Konfiguracja usług

Każdy klient Config Servera ma lokalnie tylko nazwę aplikacji i adres Config Servera. Właściwe porty, adres Eureki i trasy gatewaya są w osobnym repozytorium [`AgreeOnEat-Config`](https://github.com/MateuszKosowski/AgreeOnEat-Config), z gałęzi `main`.

Config Server domyślnie pobiera repozytorium z GitHuba. Aby w przyszłości zmienić jego adres, ustaw zmienną środowiskową `CONFIG_REPOSITORY_URI`. W Dockerze adresy między kontenerami są przekazywane przez zmienne środowiskowe. Poza Dockerem Config Server jest dostępny pod `http://localhost:8888`, a Eureka pod `http://localhost:8761/eureka/`.
