# Argentum Engine - Task Runner
# https://github.com/casey/just

# List available recipes
default:
    @just --list

# Build the entire project
build:
    ./gradlew build

# Run all tests
test:
    ./gradlew test

# Run tests for rules-engine only
test-rules:
    ./gradlew :rules-engine:test

# Run tests for game-server only
test-server:
    ./gradlew :game-server:test

# Run a specific test class (e.g., just test-class CreatureStatsTest)
test-class CLASS:
    ./gradlew :rules-engine:test --tests "{{CLASS}}"

# Start the game server
server:
    ./gradlew :game-server:bootRun

# Start the web client in dev mode
client:
    cd web-client && npm run dev

# Install web client dependencies
client-install:
    cd web-client && npm install

# Build the web client for production
client-build:
    cd web-client && npm run build

# Type check the web client
client-typecheck:
    cd web-client && npm run typecheck

# Clean build artifacts
clean:
    ./gradlew clean

# Format and check code
check:
    ./gradlew check
