# Argentum Engine - Task Runner
# https://github.com/casey/just

# List available recipes
default:
    @just --list

# Build the entire project
[group: 'build']
build:
    ./gradlew build

# Run all tests
[group: 'build']
test:
    ./gradlew test

# Run tests for rules-engine only
[group: 'build']
test-rules:
    ./gradlew :rules-engine:test

# Run tests for game-server only
[group: 'build']
test-server:
    ./gradlew :game-server:test

# Run a specific test class (e.g., just test-class CreatureStatsTest)
[group: 'build']
test-class CLASS:
    ./gradlew :rules-engine:test --tests "{{CLASS}}"

# Clean build artifacts
[group: 'build']
clean:
    ./gradlew clean

# Format and check code
[group: 'build']
check:
    ./gradlew check

# Start the game server (loads .env if present)
[group: 'dev']
server:
    @if [ -f .env ]; then set -a && source .env && set +a; fi && ./gradlew :game-server:bootRun

# Start the game server with Onslaught set enabled
[group: 'dev']
server-ons:
    @if [ -f .env ]; then set -a && source .env && set +a; fi && GAME_SETS_ONSLAUGHT_ENABLED=true ./gradlew :game-server:bootRun

# Start the web client in dev mode
[group: 'dev']
client:
    cd web-client && npm run dev

# Install web client dependencies
[group: 'dev']
client-install:
    cd web-client && npm install

# Build the web client for production
[group: 'dev']
client-build:
    cd web-client && npm run build

# Type check the web client
[group: 'dev']
client-typecheck:
    cd web-client && npm run typecheck

# Initialize local environment (copy .env.example to .env)
[group: 'env']
init:
    @if [ -f .env ]; then echo ".env already exists, skipping"; else cp .env.example .env && echo "Created .env from .env.example"; fi

# Start local Docker services (Redis)
[group: 'env']
docker-up:
    docker compose -f docker-compose.local.yml up -d

# Stop local Docker services
[group: 'env']
docker-down:
    docker compose -f docker-compose.local.yml down

# View Docker logs
[group: 'env']
docker-logs:
    docker compose -f docker-compose.local.yml logs -f

# Clear Redis data
[group: 'env']
redis-clear:
    docker exec $(docker ps -q -f ancestor=redis:7-alpine) redis-cli FLUSHALL

# Run e2e browser tests (requires server and client running, or starts them)
[group: 'test']
test-e2e:
    cd web-client && npm run test:e2e

# Run e2e browser tests with UI
[group: 'test']
test-e2e-ui:
    cd web-client && npm run test:e2e:ui

# Run e2e browser tests with visible browser
[group: 'test']
test-e2e-headed:
    cd web-client && npm run test:e2e:headed

# Run E2E scenario browser tests
[group: 'test']
test-e2e-scenarios:
    cd e2e-scenarios && npm run test

# Run E2E scenario tests with Playwright UI
[group: 'test']
test-e2e-scenarios-ui:
    cd e2e-scenarios && npm run test:ui

# Run E2E scenario tests with visible browser
[group: 'test']
test-e2e-scenarios-headed:
    cd e2e-scenarios && npm run test:headed

# Run only portal E2E scenarios
[group: 'test']
test-e2e-portal:
    cd e2e-scenarios && npm run test:portal

# Run only onslaught E2E scenarios
[group: 'test']
test-e2e-onslaught:
    cd e2e-scenarios && npm run test:onslaught

# Install E2E scenario test dependencies
[group: 'test']
e2e-scenarios-install:
    cd e2e-scenarios && npm install
