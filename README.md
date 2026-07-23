# AgreeOnEat

AgreeOnEat helps household members choose meals together. The repository currently contains a runnable microservices skeleton: configuration, service discovery, an API gateway, Keycloak, and business services protected as OAuth2 Resource Servers.

## Architecture

| Component | Responsibility |
| --- | --- |
| `config-server` | Loads service configuration from the separate Git repository [AgreeOnEat-Config](https://github.com/MateuszKosowski/AgreeOnEat-Config). |
| `discovery-server` | Eureka service registry. |
| `api-gateway` | The single public API entry point; routes requests to services through Eureka. |
| `test-web-app` | Minimal browser client for testing registration, login, and `GET /api/users/me`. |
| `user-service` | User-related API boundary. Accounts, passwords, and token issuance belong to Keycloak. |
| `household-service` | Service boundary for households, invitations, roles, and memberships. |
| `recipe-service` | Service boundary for recipes, ingredients, allergens, nutrition, cost, and preparation time. |
| `meal-planning-service` | Service boundary for meal selection, matches, elimination, and weekly planning. |
| `keycloak` | Registration, login, session management, and JWT issuance. |
| `keycloak-db` | PostgreSQL database used only by Keycloak. |

Security documentation:

- [Current authentication and authorization flow](docs/security-flow.md)
- [Reproducible Keycloak configuration and local Postman test](docs/security-keycloak.md)

## Requirements

- Docker Desktop with the Docker Engine running.

## Local setup

### 1. Start Docker Desktop

Wait until Docker Desktop reports that the Docker Engine is running.

### 2. Create a GitHub token

The Config Server reads the private `AgreeOnEat-Config` repository and needs a read-only GitHub token:

1. In GitHub, open `Settings` → `Developer settings` → `Personal access tokens` → `Fine-grained tokens`.
2. Select `Generate new token`.
3. Under **Resource owner**, choose your account. Under **Repository access**, choose `Only select repositories` and select only `AgreeOnEat-Config`.
4. Under **Repository permissions**, set `Contents` to `Read-only`.
5. Generate and copy the token immediately. GitHub displays it only once.

### 3. Prepare `.env`

From the repository root in PowerShell:

```powershell
Copy-Item .env.example .env
```

Replace the placeholder values in `.env` with your local credentials:

```text
KEYCLOAK_ADMIN_PASSWORD=your-local-password
KEYCLOAK_DB_PASSWORD=another-local-password
CONFIG_REPOSITORY_URI=https://github.com/MateuszKosowski/AgreeOnEat-Config.git
CONFIG_REPOSITORY_USERNAME=MateuszKosowski
CONFIG_REPOSITORY_TOKEN=github_pat_...
```

Do not commit `.env` or share the token.

### 4. Start the stack

```powershell
docker compose up -d --build
```

If a service starts before Config Server is ready, Docker Compose restarts it. After a few seconds, all services should appear in Eureka.

## Local addresses

| Address | Purpose |
| --- | --- |
| [http://localhost:8080](http://localhost:8080) | API Gateway |
| [http://localhost:8081](http://localhost:8081) | Keycloak admin console |
| [http://localhost:3000](http://localhost:3000) | Test web app |
| [http://localhost:8761](http://localhost:8761) | Eureka dashboard |
| [http://localhost:8888](http://localhost:8888) | Config Server |

The Keycloak admin console uses `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` from `.env`. The `agreeoneat` realm and its `agreeoneat-mobile` and `agreeoneat-web-test` clients are imported on the first start.

Open `http://localhost:3000` to test registration or login. After a successful redirect from Keycloak, the page calls `GET /api/users/me` through API Gateway and displays its JSON response. Detailed instructions are in [`test-web-app/README.md`](test-web-app/README.md).

## Contribution workflow

1. Create a GitHub Issue before starting work.
2. Create a branch for the Issue and open a Pull Request targeting `main`.
3. Add a closing reference to the PR description, for example `Closes #123`.
4. Merge only after all CI checks pass and at least one reviewer approves the PR.

Direct and force pushes to `main` are not allowed.

## Stop and reset

```powershell
docker compose down
```

The command preserves the Keycloak PostgreSQL volume. To remove all local Keycloak data, including users:

```powershell
docker compose down -v
```
