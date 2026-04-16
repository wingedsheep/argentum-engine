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

# Run tests for gym only
[group: 'build']
test-gym:
    ./gradlew :gym:test

# Run a specific test class (e.g., just test-class CreatureStatsTest)
[group: 'build']
test-class CLASS:
    ./gradlew :rules-engine:test --tests "{{CLASS}}"

# Run an engine benchmark (e.g., just benchmark, just benchmark AdvisorBenchmark 50)
[group: 'build']
benchmark CLASS="AdvisorBenchmark" GAMES="100":
    ./gradlew :rules-engine:test --tests "*.{{CLASS}}" -Dbenchmark=true -DbenchmarkGames={{GAMES}}

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
    @if [ -f .env ]; then set -a && source .env && set +a; fi && ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'

# Start the game server with Onslaught set enabled
[group: 'dev']
server-ons:
    @if [ -f .env ]; then set -a && source .env && set +a; fi && GAME_SETS_ONSLAUGHT_ENABLED=true ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'

# Start the gym HTTP server on port 8081 (for RL / MCTS training loops)
[group: 'dev']
gym-server:
    ./gradlew :gym-server:bootRun

# Run gym-server tests
[group: 'build']
test-gym-server:
    ./gradlew :gym-server:test

# Run gym-trainer tests (MCTS + self-play)
[group: 'build']
test-gym-trainer:
    ./gradlew :gym-trainer:test

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

# Start Ollama natively (GPU-accelerated, recommended for macOS)
[group: 'ai']
ollama-up:
    @echo "Starting Ollama natively (with GPU)..."
    @if ! command -v ollama &>/dev/null; then echo "Ollama not installed. Run: brew install ollama"; exit 1; fi
    ollama serve &
    @sleep 1
    @echo "Ollama running at http://localhost:11434"

# Stop native Ollama
[group: 'ai']
ollama-down:
    @pkill ollama || echo "Ollama is not running"

# Start Ollama via Docker (CPU only — no GPU on macOS Docker)
[group: 'ai']
ollama-docker-up:
    docker compose -f docker-compose.local.yml --profile ai up -d ollama

# Stop Ollama Docker container
[group: 'ai']
ollama-docker-down:
    docker compose -f docker-compose.local.yml --profile ai stop ollama

# Pull a model into Ollama (e.g., just ollama-pull qwen3:14b)
[group: 'ai']
ollama-pull MODEL:
    ollama pull {{MODEL}}

# List models available in Ollama
[group: 'ai']
ollama-models:
    ollama list

# Run all E2E browser tests
[group: 'e2e']
e2e:
    cd e2e-scenarios && npm run test

# Run E2E tests with Playwright UI
[group: 'e2e']
e2e-ui:
    cd e2e-scenarios && npm run test:ui

# Run E2E tests with visible browser
[group: 'e2e']
e2e-headed:
    cd e2e-scenarios && npm run test:headed

# Run only general E2E tests (combat, tournaments)
[group: 'e2e']
e2e-general:
    cd e2e-scenarios && npm run test:general

# Run only Portal set E2E tests
[group: 'e2e']
e2e-portal:
    cd e2e-scenarios && npm run test:portal

# Run only Onslaught set E2E tests
[group: 'e2e']
e2e-onslaught:
    cd e2e-scenarios && npm run test:onslaught

# Run E2E card tests (excludes tournament tests)
[group: 'e2e']
e2e-cards:
    cd e2e-scenarios && npx playwright test --grep-invert /Tournament/

# Run E2E tests and open HTML report with screenshots
[group: 'e2e']
e2e-report:
    cd e2e-scenarios && npx playwright test --reporter=html && npx playwright show-report

# Install E2E test dependencies
[group: 'e2e']
e2e-install:
    cd e2e-scenarios && npm install

# Watch an AI vs AI Bloomburrow match in a headed browser (requires server + client running)
# Override models: just watch-ai-match "claude-opus-4-6" "gpt-4o"
# Use LLM deck building: just watch-ai-match "z-ai/glm-5.1" "qwen/qwen3.6-plus" "false"
[group: 'e2e']
watch-ai-match MODEL1="z-ai/glm-5.1" MODEL2="qwen/qwen3.6-plus" HEURISTIC="true":
    cd e2e-scenarios && AI_MATCH=true AI_MODEL_P1={{MODEL1}} AI_MODEL_P2={{MODEL2}} AI_HEURISTIC_DECK={{HEURISTIC}} SKIP_WEB_SERVER=true npx playwright test tests/general/ai-match --headed
