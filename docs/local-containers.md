# Local containers

Build the backend first with `./mvnw package -DskipTests`, then run `docker compose up --build` from the repository root.

The compose file uses local-only development credentials. Production secrets must be supplied through the deployment environment and must not be committed.
