# The Asynchronous Input/Output & Decision Protocol

This document outlines the I/O architecture required to run the Magic: The Gathering ECS engine in a networked
environment (e.g., a WebSocket server).

## 1. The Core Concept: Engine as a State Machine

Traditional game loops often look like this (Blocking/Synchronous):

```kotlin
// BAD for servers
while (!game.isOver) {
    val action = getPlayerInput() // BLOCKS THREAD for 30 seconds!
    game.process(action)
}

```

The "Perfect" architecture treats the engine as a **Reentrant State Machine**. The engine runs until it hits a decision
point, then it **suspends** execution and returns the entire context required to resume later.

### The Execution Result

Every "Step" of the engine returns a result that indicates its status:

```kotlin
sealed interface EngineResult {
    // 1. The engine did some work and is ready for the next automatic step
    data class StateChanged(val newState: GameState, val events: List<GameEvent>) : EngineResult

    // 2. The engine stopped because a player needs to make a decision
    data class PausedForDecision(
        val state: GameState,
        val decision: PlayerDecision, // What do we need?
        val continuation: (DecisionResponse) -> EngineResult // How to resume
    ) : EngineResult

    // 3. The game has ended
    data class GameOver(val winner: EntityId?) : EngineResult
}

```

---

## 2. The Protocol Flow

This sequence demonstrates how a spell cast involving targets is handled over a network.

### Phase A: The Request (Server -> Client)

When the engine encounters a card like *Lightning Bolt*, it calculates that it needs a target.

1. The Engine creates a `ChooseTargets` decision object.
2. The Engine calculates **all valid options** (the `legalTargets` map).

* *Crucial:* The client does not calculate legality; the server sends the allowed options.


3. The Engine **pauses** and serializes the `ChooseTargets` object to JSON.
4. The JSON is sent to the specific client `playerId`.

### Phase B: The Response (Client -> Server)

1. The Client UI renders the options (highlighting valid targets).
2. The Player clicks a target.
3. The Client sends a `TargetsChoice` JSON payload back to the server.

* *Note:* This payload contains only the `decisionId` and the `selectedTargetIds`.

### Phase C: Validation & Resume (Server)

1. The Server receives the JSON.
2. **Validation:** The server checks if the `decisionId` matches the current paused state.
3. **Sanity Check:** The `DecisionValidator` ensures the selected IDs were actually in the `legalTargets` list sent
   earlier.
4. **Resume:** The server invokes the stored `continuation` with the user's input.
5. The Engine wakes up, locks in the target, and proceeds to put the spell on the stack.

---

## 3. Persistence & Crash Recovery

For a robust online client, "Pause" cannot simply rely on holding a lambda function in memory (RAM). If the server
restarts while waiting for a player, the game is lost.

### The "Serializable Continuation" Problem

Since you cannot easily serialize a Kotlin Lambda/Closure (`continuation`), the engine state must implicitly store *
*where it was**.

**Architectural Choice:**
Instead of storing a lambda, store the `PendingDecision` in the `GameState` itself (as seen in your
`pendingLegendRuleChoices`).

**The Loop:**

1. Engine checks `state.pendingDecisions`.
2. If list is not empty, **Stop**. Return `PausedForDecision`.
3. When input arrives, find the corresponding Handler (e.g., `EffectHandler`) and call `resume()`.

To support this, your `EffectHandler` must be stateless or serializable.

---

## 4. Timeouts and Priority Handling

In an online environment, you cannot wait forever.

### The Priority Timer

The `AsyncPlayerInterface` wraps the decision request in a timeout wrapper.

```kotlin
// Pseudocode for the Server Manager
fun handleGameLoop(gameId: String) {
    val result = engine.process()

    when (result) {
        is PausedForDecision -> {
            // Send to client
            socket.send(result.decision)

            // Start Timer
            timer.schedule(30.seconds) {
                // If no response, force a default action
                val defaultResponse = AutoPlayerInterface.defaultResponse(result.decision)
                engine.resume(defaultResponse)
            }
        }
    }
}

```

### "F6" (Auto-Pass) Logic

The protocol should support **Client-Side Yielding**.

* The client sends a "Auto-Pass Priority" flag.
* The Server's `LegalActionCalculator` checks this flag.
* If `PriorityDecision` comes up and the flag is set, the server **automatically generates** a `Pass` response
  immediately, without sending a packet to the client.

---

## 5. Client-Side Prediction (The "Feel" of the game)

While the Server is the authority, waiting 200ms for the server to say "You can cast this" feels sluggish.

**The Hybrid Approach:**

1. **Shared Logic:** The Client runs a simplified version of the ECS Engine (the `TargetValidator` and
   `LegalActionCalculator`).
2. **Optimistic UI:** When the player drags *Lightning Bolt*, the Client validator creates the targeting arrows locally.
3. **Authority Check:** When released, the client sends the Action. The Server validates. If invalid (rare race
   condition), the server sends a "State Correction" packet (Lag Compensation) resetting the client's board.

---

## 6. Security: The Fog of War

The I/O protocol must never send the full `GameState` to the client.

**The View Projector:**
Before sending an update to Player A, the `GameState` runs through a `SecurityFilter`:

1. **Mask Hands:** Opponent's hand becomes a list of empty Card objects (count only).
2. **Mask Face-Down:** Face-down cards (Morphs/Manifests) are stripped of their `CardDefinition` and replaced with a
   generic "2/2 Morph" definition.
3. **Mask Library:** Library order is hidden (except known top cards).

This ensures that packet sniffing cannot reveal the opponent's hand.
