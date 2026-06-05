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

# CLASS options (all in :ai): AdvisorBenchmark   - AI advisor vs random, per-card timing
#                             GameBenchmark      - full AI-vs-AI games, sealed decks
#                             RandomActionBenchmark - raw engine throughput (see benchmark-random)
#                             StateCloneBenchmark   - GameState clone speed (uses -DbenchmarkIterations, not GAMES)
# Run an engine benchmark (e.g., just benchmark, just benchmark GameBenchmark 50)
[group: 'build']
benchmark CLASS="AdvisorBenchmark" GAMES="100":
    ./gradlew :ai:test --tests "*.{{CLASS}}" -Dbenchmark=true -DbenchmarkGames={{GAMES}}

# Run the random-action engine throughput benchmark on a set (e.g., just benchmark-random 200 BLB)
[group: 'build']
benchmark-random GAMES="100" SET="POR":
    ./gradlew :ai:test --tests "*.RandomActionBenchmark" -Dbenchmark=true -DbenchmarkGames={{GAMES}} -DbenchmarkSet={{SET}}

# Clean build artifacts
[group: 'build']
clean:
    ./gradlew clean

# Format and check code
[group: 'build']
check:
    ./gradlew check

# Report implemented vs missing cards for a set (e.g., just card-status --set BLB --list)
[group: 'build']
card-status *ARGS:
    scripts/card-status {{ARGS}}

# Build the Kotlin coverage tooling once so the recipes below can call its CLI (fast no-op when
# up to date). The bridge + lenses live in the :mtgish-tooling module (Kotlin port of the mtgish spike).
_coverage-tool:
    @./gradlew -q --console=plain :mtgish-tooling:installDist

# Predict engine coverage via the mtgish bridge — which missing cards are free vs blocked.
# Whole set:   just coverage --set TMP            (implemented / FREE / blocked + leaderboard)
#              just coverage --set TMP --free     (also list the free-to-implement cards)
#              just coverage --set TMP --blocked  (also list blocked cards + reasons)
# One card:    just coverage --card "Shivan Dragon"
# Trust check: just coverage --calibrate POR      (implemented cards must classify coverable)
[group: 'build']
coverage *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling probe {{ARGS}}

# Generation fidelity — could we AUTO-AUTHOR a card from mtgish? Diffs the bridge's output
# against each card's compiled golden snapshot, tiering AUTO / SCAFFOLD / MISS.
# Whole set:  just coverage-fidelity --set POR
#             just coverage-fidelity --set POR --list SCAFFOLD
# Cross-set:  just coverage-fidelity --all          (generalization table — bridge applied unchanged)
# One card:   just coverage-fidelity --emit "Shivan Dragon"   (prints generated cardDef DSL)
[group: 'build']
coverage-fidelity *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling fidelity {{ARGS}}

# Auto-gen gap: of a set's UNIMPLEMENTED cards, how many could the bridge draft now?
#   just coverage-gaps --set TMP                 # AUTOGEN / SCAFFOLD / BLOCKED + leaderboard
#   just coverage-gaps --set TMP --list AUTOGEN  # list the draftable cards
[group: 'build']
coverage-gaps *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --gaps {{ARGS}}

# Generate draft .kt files for a set's AUTOGEN-predicted missing cards into a STAGING dir.
# DRAFTS ONLY — they must compile + pass a scenario test + be reviewed before use.
#   just coverage-generate --set TMP             # -> spike/mtgish-coverage/generated/tmp/
[group: 'build']
coverage-generate *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --write {{ARGS}}

# Replace a set's real card sources with mtgish-generated files, including scaffold files for
# structures the emitter deliberately declines to auto-author. Intended for calibrated set refreshes.
#   just coverage-refresh-set POR
[group: 'build']
coverage-refresh-set SET: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --write-all --set {{SET}}

# COMPILE-VERIFICATION GATE — the real proof that AUTO cards are emittable, not just predicted.
# Emits every whole-renderable card of a set into an isolated Gradle source set, COMPILES them,
# serialises each via the same CardExporter the golden snapshots use, then gameplay-tree diffs vs
# golden. PASS = every emitted card compiles and matches golden (0 mismatch); also reports how
# many of the set it auto-emits. Portal: 184/184 emitted & verified, 0 mismatch.
#   just coverage-verify --set POR
[group: 'build']
coverage-verify SET="POR": _coverage-tool
    ./gradlew :mtg-sets:verifyGeneratedCards -Pset={{SET}} --console=plain --rerun-tasks
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling fidelity --gate {{SET}}

# Verify backlog/sets/*/cards.md headers match actual [x] / [x]+[ ] counts
[group: 'build']
check-backlog:
    scripts/check-card-counts.py --check

# Rewrite backlog/sets/*/cards.md headers to match actual [x] / [x]+[ ] counts
[group: 'build']
fix-backlog:
    scripts/check-card-counts.py --fix

# Verify every backlog [ ] entry is genuinely unimplemented (cross-checks Kotlin sources)
[group: 'build']
check-backlog-implementations:
    scripts/check-backlog-implementations.py --check

# Tick [x] for backlog entries that already have a CardDefinition or Printing
[group: 'build']
fix-backlog-implementations:
    scripts/check-backlog-implementations.py --fix
    scripts/check-card-counts.py --fix

# Validate a single card's printings against Scryfall: canonical must live in the
# card's earliest real-expansion printing; every other scaffolded printing must
# have a reprint row. Strict — if the earliest set isn't scaffolded, that's drift.
[group: 'build']
check-card-printing CARD:
    scripts/check-card-printing.py "{{CARD}}"

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

# Install Playwright browser binaries (one-time, after e2e-install)
[group: 'e2e']
e2e-install-browsers:
    cd e2e-scenarios && npx playwright install chromium

# Run a specific E2E test by path or grep pattern (e.g., just e2e-test sparksmith)
[group: 'e2e']
e2e-test PATTERN:
    cd e2e-scenarios && npx playwright test {{PATTERN}}

# Run E2E tests under the Playwright Inspector (step through, inspect selectors)
[group: 'e2e']
e2e-debug PATTERN="":
    cd e2e-scenarios && npx playwright test {{PATTERN}} --debug

# Open Playwright codegen against the local client to record selectors and actions
[group: 'e2e']
e2e-codegen URL="http://localhost:5173":
    cd e2e-scenarios && npx playwright codegen {{URL}}

# Open a trace file produced by a failed run (e.g., just e2e-trace test-results/.../trace.zip)
[group: 'e2e']
e2e-trace TRACE:
    cd e2e-scenarios && npx playwright show-trace {{TRACE}}

# Watch an AI vs AI match in a headed browser. Params (all optional):
#   MODEL1, MODEL2   — LLM model ids; pass "" "" for built-in engine AIs (no LLM calls)
#   SETS             — set code or comma-separated list, e.g. "BLB" or "ONS,LGN,SCG"
#   HEURISTIC        — "true" (fast heuristic deck build) or "false" (LLM deck build)
#   PROFILE          — "true" enables React render profiler; report prints at the end
# Examples:
#   just watch-ai-match                                      # default BLB match with LLMs
#   just watch-ai-match "" ""                                # engine-vs-engine BLB
#   just watch-ai-match "" "" "KTK"                          # engine-vs-engine Khans
#   just watch-ai-match "" "" "ONS,LGN,SCG"                  # engine-vs-engine Onslaught block
#   just watch-ai-match "" "" "BLB" "true" "true"            # engine-vs-engine BLB with profiler
[group: 'e2e']
[doc("AI vs AI match in a headed browser — params: MODEL1 MODEL2 SETS HEURISTIC PROFILE (pass \"\" \"\" for engine-vs-engine)")]
watch-ai-match MODEL1="z-ai/glm-5.1" MODEL2="qwen/qwen3.6-plus" SETS="BLB" HEURISTIC="true" PROFILE="false":
    cd e2e-scenarios && AI_MATCH=true AI_MODEL_P1={{MODEL1}} AI_MODEL_P2={{MODEL2}} AI_HEURISTIC_DECK={{HEURISTIC}} AI_SET_CODES={{SETS}} PROFILE={{PROFILE}} SKIP_WEB_SERVER=true npx playwright test tests/general/ai-match --headed

# Watch an engine-vs-engine AI match using two fixed pre-built decks. Sealed pool +
# deckbuilding are skipped entirely. Each deck JSON is a `{ "Card Name": count }` object;
# paths are resolved relative to the current working directory.
# Examples:
#   just watch-ai-match-decks e2e-scenarios/decks/uw-tempo.json e2e-scenarios/decks/standard-monou.json
#   just watch-ai-match-decks decks/p1.json decks/p2.json "" ""                   # built-in engine AIs (no LLMs)
#   just watch-ai-match-decks decks/p1.json decks/p2.json "anthropic/claude-..." ""
[group: 'e2e']
[doc("AI vs AI match with two fixed deck JSONs — params: DECK1 DECK2 [MODEL1 MODEL2 PROFILE]")]
watch-ai-match-decks DECK1 DECK2 MODEL1="" MODEL2="" PROFILE="false":
    #!/usr/bin/env bash
    set -euo pipefail
    # Resolve deck paths against the caller's cwd before chdir into e2e-scenarios so
    # relative paths like `e2e-scenarios/decks/uw-tempo.json` keep working.
    DECK1_ABS="$(cd "$(dirname "{{DECK1}}")" && pwd)/$(basename "{{DECK1}}")"
    DECK2_ABS="$(cd "$(dirname "{{DECK2}}")" && pwd)/$(basename "{{DECK2}}")"
    cd e2e-scenarios
    AI_MATCH=true AI_DECK_P1="$DECK1_ABS" AI_DECK_P2="$DECK2_ABS" AI_MODEL_P1="{{MODEL1}}" AI_MODEL_P2="{{MODEL2}}" PROFILE="{{PROFILE}}" SKIP_WEB_SERVER=true npx playwright test tests/general/ai-match --headed
