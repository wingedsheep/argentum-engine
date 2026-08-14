# Argentum Engine - Task Runner
# https://github.com/casey/just

# List available recipes
default:
    @just --list

# Build the entire project
[group: 'build']
build:
    scripts/gradle-locked build

# Run all tesats
[group: 'build']
test:
    scripts/gradle-locked test

# Run tests for rules-engine only
[group: 'build']
test-rules:
    scripts/gradle-locked :rules-engine:test

# Run tests for game-server only
[group: 'build']
test-server:
    scripts/gradle-locked :game-server:test

# Run tests for the AI module only (advisors, deckbuild/draft heuristics)
[group: 'build']
test-ai:
    scripts/gradle-locked :ai:test

# Run tests for gym only
[group: 'build']
test-gym:
    scripts/gradle-locked :gym:test

# Run a specific test class (e.g., just test-class CreatureStatsTest)
[group: 'build']
test-class CLASS:
    scripts/gradle-locked :rules-engine:test --tests "{{CLASS}}"

# Re-bless per-set card snapshot goldens after an intentional change (review the diff: only your cards should move)
[group: 'build']
rebless-cards:
    scripts/gradle-locked :mtg-sets:test --tests "*CardDefinitionSnapshotTest" -DupdateSnapshots=true

# List every token our cards create that has no set-scoped art, so it renders with generic
# stand-in art. Writes backlog/token-art-gaps.md with a suggested image path and a paste-ready
# TokenPrinting row per gap. Mostly pre-2001 sets, which have no Scryfall token set to sync.
[group: 'build']
token-art-gaps:
    scripts/gradle-locked :mtg-sets:tokenArtGaps

# Refresh mtg-sets/src/main/resources/tokens.json from Scryfall's token sets (t<code>). Hand-authored
# art belongs in a set's `tokenArt` (which wins over synced rows) — this file is regenerated wholesale.
[group: 'build']
token-art-sync:
    scripts/gradle-locked :mtg-sets:syncTokenArt

# CLASS options (all in :ai): AdvisorBenchmark   - AI advisor vs random, per-card timing
#                             GameBenchmark      - full AI-vs-AI games, sealed decks
#                             RandomActionBenchmark - raw engine throughput (see benchmark-random)
#                             SimulationThroughputBenchmark - AI-game process/simulate/projection rates
#                                                     and branching factor (see benchmark-throughput)
#                             StateCloneBenchmark   - GameState clone speed (uses -DbenchmarkIterations, not GAMES)
# Run an engine benchmark (e.g., just benchmark, just benchmark GameBenchmark 50)
[group: 'build']
benchmark CLASS="AdvisorBenchmark" GAMES="100":
    ./gradlew :ai:test --tests "*.{{CLASS}}" -Dbenchmark=true -DbenchmarkGames={{GAMES}}

# Run the random-action engine throughput benchmark on a set (e.g., just benchmark-random 200 BLB)
[group: 'build']
benchmark-random GAMES="100" SET="POR":
    ./gradlew :ai:test --tests "*.RandomActionBenchmark" -Dbenchmark=true -DbenchmarkGames={{GAMES}} -DbenchmarkSet={{SET}}

# Measure what a rollout evaluator can afford: process()/simulate()/projection rates
# and branching factor over real AI games (e.g., just benchmark-throughput 40 BLB).
# Baseline numbers live in docs/ai/baseline-metrics.md.
[group: 'build']
benchmark-throughput GAMES="20" SET="BLB":
    ./gradlew :ai:test --tests "*.SimulationThroughputBenchmark" -Dbenchmark=true -DbenchmarkGames={{GAMES}} -DbenchmarkSet={{SET}}

# Play two AI agents head-to-head over paired-swap games and report a win rate with a confidence
# interval (e.g., just arena v0 blb-advisors 1000). Agents: v0, current, production, blb-advisors,
# ons-advisors, v0-blind. 1000 games is the merge gate; 300 is directional; 100 is a smoke test.
# Results land in benchmarks/arena/. How to read one: docs/ai/measurement.md.
[group: 'ai']
arena A B GAMES="300" SET="BLB" SEED="20260727" ARTIFACT_DIR="":
    scripts/gradle-locked :ai:test --tests "*.ArenaBenchmark" -Dbenchmark=true -Darena=true \
        -DarenaA={{A}} -DarenaB={{B}} -DarenaGames={{GAMES}} -DarenaSet={{SET}} -DarenaSeed={{SEED}} \
        -Dargentum.ai.apprentice.dir={{ARTIFACT_DIR}}

# ECL apprentice promotion ladder. Artifacts are installed outside the repository and selected with
# -Dargentum.ai.apprentice.dir; missing or invalid files safely use the production evaluator.
[group: 'ai']
arena-ecl-smoke ARTIFACT_DIR GAMES="100" SEED="20260801":
    just arena ecl-apprentice production {{GAMES}} ECL {{SEED}} {{ARTIFACT_DIR}}

[group: 'ai']
arena-ecl-directional ARTIFACT_DIR GAMES="300" SEED="20260801":
    just arena ecl-apprentice production {{GAMES}} ECL {{SEED}} {{ARTIFACT_DIR}}

# Fit the dependency-free linear apprentice from a pairwise-example JSON file.
[group: 'ai']
train-ecl-apprentice EXAMPLES OUTPUT:
    python3 scripts/train_ecl_apprentice.py {{EXAMPLES}} {{OUTPUT}}

# Collect clean ECL games. Failed/recovered games are quarantined and never appended.
[group: 'ai']
collect-ecl-training OUTPUT GAMES="100" SEED="20260801" RUN_ID="ecl-{{SEED}}":
    scripts/gradle-locked :ai:test --tests "*.EclTrainingBenchmark" -Dbenchmark=true -DeclCollect=true \
        -DeclCollectGames={{GAMES}} -DeclCollectSeed={{SEED}} -DeclCollectOutput={{OUTPUT}} \
        -DeclCollectBaseDir={{justfile_directory()}} -DeclCollectRunId={{RUN_ID}}

# Play the rollout evaluator against itself at 4 / 8 / 16 / 32 playouts per decision. Same claim as
# arena-budget-scaling one level down: strength must never FALL with more playouts, or the search is
# generating noise. Measured: it rises to ~8 and then plateaus, which is why NORMAL_PLAYOUTS is 16.
# Also how you afford a rollout arena at all — a rollout game is ~50x a v0 game.
# Pick rungs far apart: 4-vs-8 is below this harness's resolution (see docs/ai/measurement.md).
[group: 'ai']
arena-rollout-scaling A="v0-rollout-4" B="v0-rollout-32" GAMES="100" SET="BLB" SEED="20260727":
    just arena {{A}} {{B}} {{GAMES}} {{SET}} {{SEED}}

# Run the 66-puzzle tactical suite (11 categories x 6). Seconds, not minutes: the arena says *that*
# the AI regressed, a puzzle category says *what*. The gate is "the failing set equals
# KNOWN_FAILURES", so an unexpected fix fails the test too. Baseline: docs/ai/baseline-metrics.md.
[group: 'ai']
arena-puzzles:
    scripts/gradle-locked :ai:test --tests "*.PuzzleSuiteTest"

# Same 66 puzzles across AI profiles (v0, production) with a side-by-side per-category table.
[group: 'ai']
arena-puzzles-compare:
    scripts/gradle-locked :ai:test --tests "*.PuzzleComparisonBenchmark" -Dbenchmark=true

# Play one agent against a field of another at a multiplayer table and report a win share with a
# confidence interval (e.g. just arena-pod ffa3 current v0-blind 300). Tables: ffa3, ffa4, 2hg.
# NOTE the null hypothesis is 1/teams — 33% at ffa3, 25% at ffa4, 50% at 2hg — not 50% everywhere.
# Results land in benchmarks/arena/. How to read one: docs/ai/measurement.md.
[group: 'ai']
arena-pod TABLE A B GAMES="300" SET="BLB" SEED="20260727":
    scripts/gradle-locked :ai:test --tests "*.ArenaBenchmark" -Dbenchmark=true -DarenaPod=true \
        -DarenaTable={{TABLE}} -DarenaA={{A}} -DarenaB={{B}} -DarenaGames={{GAMES}} \
        -DarenaSet={{SET}} -DarenaSeed={{SEED}}

# Run every agent in ai/src/test/resources/arena/gauntlet.json against every other and print the
# full pairwise matrix plus Bradley-Terry Elo. The matrix is the deliverable — MTG agents are
# frequently non-transitive, and a single rating erases exactly that.
[group: 'ai']
arena-gauntlet GAMES="200" SET="BLB" SEED="20260727":
    scripts/gradle-locked :ai:test --tests "*.ArenaBenchmark" -Dbenchmark=true -DarenaGauntlet=true \
        -DarenaGames={{GAMES}} -DarenaSet={{SET}} -DarenaSeed={{SEED}}

# Play the same agent against itself at 100 / 1000 / 3000 ms of decision budget. Strength must be
# MONOTONE in the budget; if it isn't, the search is generating noise and the fix is a better leaf
# evaluator (phases 6 and 9), not more samples. Runs four matchups, so budget 4x GAMES.
[group: 'ai']
arena-budget-scaling GAMES="300" SET="BLB" SEED="20260727":
    scripts/gradle-locked :ai:test --tests "*.ArenaBudgetScalingTest" -Dbenchmark=true \
        -DarenaBudgetScaling=true -DarenaGames={{GAMES}} -DarenaSet={{SET}} -DarenaSeed={{SEED}}

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

# Interactive coverage dashboard (TUI) — navigate every set's implemented / free-to-add / blocked
# breakdown + feature leaderboard, drill into the card list and per-card capability verdict, and
# press `c` for the cross-set "what engine work unlocks the most cards everywhere" ranking.
#   just coverage-dashboard            # lazy — sets analyze as you visit them
#   just coverage-dashboard --scan     # analyze every set up front (fills all +N counts + global total)
#   ↑↓ navigate · → drill in · ← back · tab Kotlin/capabilities · / filter · s sort · f scan-all · q quit
[group: 'build']
coverage-dashboard *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling dashboard {{ARGS}}

# Cross-set capability index, non-interactively — the same ranking as the dashboard's `c` view
# ("what engine work unlocks the most cards everywhere"), printed as a plain table so it can be
# piped, diffed, or pasted into a backlog doc. Use the TUI when you want to DRILL into the cards.
#   just coverage-cross              # every capability, ranked by blocked cards it would unlock
#   just coverage-cross --top 50     # just the head of the ranking
[group: 'build']
coverage-cross *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling dashboard --cross {{ARGS}}

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
#   just coverage-generate --set TMP             # -> mtgish-tooling/generated/tmp/
[group: 'build']
coverage-generate *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --write {{ARGS}}

# Replace a set's real card sources with mtgish-generated files, including scaffold files for
# structures the emitter deliberately declines to auto-author. Intended for calibrated set refreshes.
#   just coverage-refresh-set POR                   # all cards (whole renders + scaffolds)
#   just coverage-refresh-set POR --complete-only   # only confidently-whole renders; skip scaffolds
[group: 'build']
coverage-refresh-set SET *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --write-all --set {{SET}} {{ARGS}}

# Relocate misplaced canonicals: for every card in SET whose earliest real printing is a DIFFERENT
# set, emit the canonical card(...) into that earlier set's cards/ package (with its own metadata),
# so SET can safely carry only Printing(...) rows. New earlier sets still need an MtgSet object +
# MtgSetCatalog entry. DRAFTS — compile + scenario-test + review before relying on them.
#   just coverage-relocate POR
[group: 'build']
coverage-relocate SET: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling autogen --relocate --set {{SET}}

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

# Vendored emitter-regression fixtures — a small committed slice of the mtgish IR + Scryfall metadata
# for the calibrated sets, plus a golden of the emitter's output. The in-suite EmitterGoldenTest
# re-emits from the slice and diffs the golden (hermetic — no IR download, no Gradle compile, runs in
# `just test`), so a bridge/handler change that silently alters a card fails fast with the exact card.
#   just coverage-fixtures POR            # refresh slice + golden from real data (needs IR + cache)
#   just coverage-fixtures --rebless      # re-render golden from committed slices (intentional emitter change)
[group: 'build']
coverage-fixtures *ARGS: _coverage-tool
    @mtgish-tooling/build/install/mtgish-tooling/bin/mtgish-tooling fixtures {{ARGS}}

# Full emitter regression in one target: the fast hermetic golden test, then the compile + gameplay-
# tree GATE for every set currently at 0-mismatch. Add a set to the loop once `coverage-verify <SET>`
# passes it clean.
[group: 'build']
coverage-verify-all: _coverage-tool
    #!/usr/bin/env bash
    set -euo pipefail
    echo "== emitter golden (hermetic, all committed fixtures) =="
    ./gradlew :mtgish-tooling:test --tests "*EmitterGoldenTest" --console=plain
    for s in POR ISD; do
        echo "== coverage-verify $s (compile + gameplay-tree gate) =="
        just coverage-verify "$s"
    done

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
    @if [ -f .env ]; then set -a && . ./.env && set +a; fi && ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'

# Start the game server and web client together
[group: 'dev']
dev:
    #!/usr/bin/env bash
    set -euo pipefail
    just server &
    server_pid=$!
    just client &
    client_pid=$!
    trap 'kill "$server_pid" "$client_pid" 2>/dev/null || true; wait "$server_pid" "$client_pid" 2>/dev/null || true' EXIT INT TERM
    wait "$server_pid" "$client_pid"

# Start the game server with Onslaught set enabled
[group: 'dev']
server-ons:
    @if [ -f .env ]; then set -a && . ./.env && set +a; fi && GAME_SETS_ONSLAUGHT_ENABLED=true ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'

# Start the gym HTTP server on port 8081 (for RL / MCTS training loops)
[group: 'dev']
gym-server:
    ./gradlew :gym-server:bootRun

# Run gym-server tests
[group: 'build']
test-gym-server:
    scripts/gradle-locked :gym-server:test

# Run gym-trainer tests (MCTS + self-play)
[group: 'build']
test-gym-trainer:
    scripts/gradle-locked :gym-trainer:test

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
