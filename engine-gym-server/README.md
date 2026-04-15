# `:engine-gym-server`

Spring Boot HTTP transport on top of [`:engine-gym`](../engine-gym/README.md)'s
`MultiEnvService`. Intended for Python-driven training loops — the kind
where the trainer owns the play loop and the engine just advances states.

If your trainer lives in the JVM and drives the engine directly, you
don't need this module; use `:engine-gym` and
[`:engine-gym-trainer`](../engine-gym-trainer/README.md) instead.

## Why this module exists

`:engine-gym` is transport-agnostic on purpose. An HTTP shell is one of
*several* things that can expose it (gRPC, WebSocket, RSocket, ZeroMQ
are all plausible) and each would pull different dependencies. Keeping
the transport in its own module means:

- `:engine-gym` stays a pure library that can be embedded in a JVM
  trainer with zero Spring / Tomcat footprint.
- A trainer that wants HTTP adds this module and gets the endpoints,
  exception mapping and JSON codec wiring for free.
- Projects that want their own transport (MessagePack over TCP,
  Protobuf over gRPC) fork *this* module, not the service layer.

## Endpoints

Default port **8081** so it coexists with the game server on 8080.

| Method & path | Wraps | Body / query |
|---|---|---|
| `POST /envs` | `MultiEnvService.create` | `EnvConfig` JSON |
| `GET /envs` | `listEnvs` | — |
| `DELETE /envs` | `dispose` | `{ "envIds": [...] }` |
| `GET /envs/{id}` | `observe` | `?revealAll=true` optional |
| `POST /envs/{id}/reset` | `reset` | `EnvConfig` JSON |
| `POST /envs/{id}/step` | `step` | `{ "actionId": 3 }` |
| `POST /envs/step-batch` | `stepBatch` (parallel) | `[ { envId, actionId }, ...]` |
| `POST /envs/{id}/decision` | `submitDecision` | `DecisionResponse` JSON |
| `POST /envs/{id}/fork` | `fork` | `?count=N` |
| `POST /envs/{id}/snapshot` | `snapshot` | — |
| `POST /envs/{id}/restore` | `restore` | `SnapshotHandle` JSON |
| `GET /schema-hash` | constant | returns `{ schemaHash }` for drift-check |
| `GET /health` | constant | returns `{ status: "ok" }` |

## Design choices worth knowing about

### kotlinx.serialization, not Jackson

`WebConfig` registers `KotlinSerializationJsonHttpMessageConverter` as
the primary JSON converter. Every type on the wire (`EnvConfig`,
`DeckSpec`, `DecisionResponse`, `TrainingObservation`,
`SnapshotHandle`, …) is already `@Serializable` in `:engine-gym`, and
sealed hierarchies use `@SerialName` discriminators — the converter
handles polymorphism natively. Switching to Jackson would have required
parallel `@JsonTypeInfo`/`@JsonSubTypes` annotations for every sealed
type.

The default Jackson converter stays in the chain for exception responses
and anything that isn't `@Serializable`.

### Real HTTP in tests, not MockMvc

Spring Boot 4.0 dropped `@AutoConfigureMockMvc` and `TestRestTemplate`
from the default `spring-boot-starter-test` classpath. The integration
tests therefore boot the full Spring app on a random port
(`WebEnvironment.RANDOM_PORT`) and drive it with Java's
`java.net.http.HttpClient`. That happens to be a better test anyway —
the real HTTP conversion chain, the real exception handlers, the real
ports — but it's worth knowing why there's no MockMvc setup.

### Exception → status code mapping

`GymExceptionHandler` maps the three exceptions `MultiEnvService` can
throw onto meaningful HTTP codes so Python clients can distinguish
operator mistakes from server faults:

| Exception | HTTP | When |
|---|---|---|
| `NoSuchElementException` | 404 | Unknown envId, missing snapshot |
| `IllegalArgumentException` | 400 | Bad deck, stale action ID, unknown set code |
| `IllegalStateException` | 409 | `submitDecision` when no decision is pending |

Anything else propagates as 500.

### Action IDs stay per-step

The server does not stabilise action IDs across steps. Every `step`
regenerates the `ActionRegistry`, and IDs from a prior observation
become invalid. This matches the `:engine-gym` contract — see its README
for the rationale — and the test suite exercises the failure mode so a
trainer that holds onto stale IDs fails loudly (400).

### No authentication, no TTLs, no metrics

Deliberately out of scope for the current scaffold, flagged in
`GymServerApplication.kt`:

- **Auth.** Bind to localhost until you add a bearer-token filter or
  network ACL.
- **Env lifetime / TTLs.** A crashed trainer leaks envs forever — the
  natural next step is a reaper thread + heartbeat header.
- **Byte-based snapshots.** `SnapshotHandle.Slot` is in-process only;
  the sealed interface has room for a `Bytes` variant once cross-process
  MCTS workers become a thing.
- **Metrics / structured logs.** Add Prometheus or similar at the Spring
  level when you need them.

### Set catalogue is configurable

`GymBeansConfig` wires a default `CardRegistry` (Portal + Bloomburrow
with basic-land variants) and a default `BoosterGenerator` (Bloomburrow).
Override the two `@Bean` methods in your own `@Configuration` to add
sets — the `MultiEnvService` bean will pick them up automatically.

## Running it

```bash
just gym-server                # default config, port 8081
./gradlew :engine-gym-server:bootRun
```

The process has no persistence — env state is all in-memory. Restart
clears everything.

## Interactive docs — Swagger UI

Every endpoint is documented in-process. Once the server is running:

- `http://localhost:8081/swagger-ui.html` — browsable UI with request bodies, response schemas, and a "Try it out" button wired to the real service
- `http://localhost:8081/v3/api-docs` — the raw OpenAPI 3 JSON spec

This is the fastest way to verify a Python client's payload shape —
`POST /envs` in the UI, inspect the response, then copy the wire bytes
into your client.

Disable the UI in any deployment where the endpoint surface should not
be discoverable:

```bash
GYM_OPENAPI_ENABLED=false ./gradlew :engine-gym-server:bootRun
```

## Tests

```bash
just test-gym-server                    # this module only
./gradlew :engine-gym-server:test       # same via gradle
```

7 integration tests covering the happy path (`create → observe → step →
dispose` round-trip), error paths (unknown env → 404, unknown set code
→ 400, stale action ID → 400), schema hash / health, and multi-step
turn advancement.

## When to use this instead of `:engine-gym-trainer`

- Python owns the trainer loop → this module.
- Your existing Python training stack uses OpenAI Gym / Gymnasium /
  PettingZoo conventions → this module + a thin Python `Env` wrapper.
- You're running a distributed setup where inference workers and rollout
  workers are on different machines → this module.
- The trainer is JVM-side and embeds `:engine-gym` directly → skip this
  module, use `:engine-gym-trainer`.
