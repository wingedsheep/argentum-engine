# Developer Guide: How to Extend Argentum

## 1. Adding a New Card Set
You do **not** modify `rules-engine` core to add cards.

1.  Create a new Gradle module: `sets/set-alpha`.
2.  Implement `CardRegistry`:
    ```kotlin
    object AlphaSet : CardRegistry {
        override val setCode = "LEA"
        override fun getCards() = listOf(
            BlackLotus.definition,
            GiantGrowth.definition
        )
    }
    ```
3.  Implement Card Scripts using the DSL:
    ```kotlin
    val BlackLotus = card("Black Lotus") {
        type("Artifact")
        activatedAbility("{T}, Sacrifice {self}") {
            effect(AddMana.ofAnyColor(3))
        }
    }
    ```
4.  Add the module as a dependency to `game-server`. The loader will find it automatically.

## 2. Adding a New Mechanic (e.g., Scry)
This requires modifying `rules-engine`.

1.  **Define the Effect:** Add `ScryEffect(amount: Int)` to `Effect.kt`.
2.  **Write the Handler:** Create `ScryHandler.kt`.
    *   This handler needs to return a `PendingDecision` because the user needs to choose top/bottom orders.
3.  **Register the Handler:** Add to `EffectHandlerRegistry`.
4.  **Update Client (Optional):** If `Scry` requires a unique UI (like ordering cards visually), update `web-client` to handle the `DecisionRequest` for reordering.

## 3. Deployment Strategy
For "Me and my friends":

1.  **Build:** Run `./gradlew build`. This produces `game-server.jar`.
2.  **Build Frontend:** Run `npm run build` in `web-client`.
3.  **Dockerize:** Create a Docker Compose file.
    *   `postgres`: Database for decks/users.
    *   `app`: Runs the `game-server.jar`.
    *   `nginx`: Serves the static `web-client` files and proxies `/api` and `/ws` to the app container.
