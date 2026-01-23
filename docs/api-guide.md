# Developer Guide: How to Extend Argentum

This guide provides step-by-step instructions for the most common development tasks: adding content (cards) and adding capabilities (mechanics).

## 1. Adding a New Card Set

To add new cards, you work exclusively in the **`mtg-sets`** module. We use a folder-based structure to keep cards organized.

### Step 1: Create the Folder Structure

Create a package for your set inside `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/`.

```text
definitions/
  └───custom/
      ├───cards/          <-- Individual card files go here
      │   ├───ThunderStrike.kt
      │   └───MysticBarrier.kt
      └───MyNewSet.kt     <-- The set definition file

```

### Step 2: Define Individual Cards

Create a separate Kotlin file for each card in the `cards/` directory. Use the `cardDef` builder.

**File:** `definitions/custom/cards/ThunderStrike.kt`

```kotlin
package com.wingedsheep.mtg.sets.definitions.custom.cards

import com.wingedsheep.mtg.sdk.dsl.cardDef
import com.wingedsheep.mtg.sdk.scripting.effect.Effects
import com.wingedsheep.mtg.sdk.scripting.target.TargetFilter
import com.wingedsheep.mtg.sdk.core.enums.Rarity

// Define the card as a public val
val ThunderStrike = cardDef("Thunder Strike") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    
    metadata {
        rarity = Rarity.COMMON
        flavorText = "Zap!"
    }

    spell {
        effect = Effects.DealDamage(3)
        target = TargetFilter.Any
    }
}

```

**File:** `definitions/custom/cards/MysticBarrier.kt`

```kotlin
package com.wingedsheep.mtg.sets.definitions.custom.cards

import com.wingedsheep.mtg.sdk.dsl.cardDef
// ... imports

val MysticBarrier = cardDef("Mystic Barrier") {
    manaCost = "{W}"
    typeLine = "Enchantment"
    // ... logic
}

```

### Step 3: Define the Set

Create the set definition file that aggregates the cards.

**File:** `definitions/custom/MyNewSet.kt`

```kotlin
package com.wingedsheep.mtg.sets.definitions.custom

import com.wingedsheep.mtg.sdk.dsl.cardSet
import com.wingedsheep.mtg.sets.definitions.custom.cards.* // Import your cards

val MyNewSet = cardSet("MNS", "My New Set") {
    // Register the cards defined in the other files
    add(ThunderStrike)
    add(MysticBarrier)
}

```

### Step 4: Register the Set

Open `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/provider/SetRegistry.kt`.
Add your new set to the list of available providers.

```kotlin
object SetRegistry {
    val allSets = listOf(
        PortalSet,
        AlphaSet,
        MyNewSet // <--- Add this line
    )
}

```

### Step 5: Verify

Run the server. The `SetLoader` will automatically discover and load your new cards.

---

## 2. Adding a New Mechanic (e.g., "Scry")

Adding a new mechanic requires touching all three backend modules.

### Step 1: Define the Vocabulary (`mtg-sdk`)

Open `mtg-sdk/src/main/kotlin/.../scripting/effect/Effects.kt`.
Add the data class representing the effect.

```kotlin
@Serializable
sealed interface Effect {
    // ... existing effects
    
    @Serializable
    data class Scry(val amount: Int) : Effect
}

```

### Step 2: Implement the Logic (`mtg-engine`)

Open `mtg-engine/src/main/kotlin/.../handlers/`.
Create a new handler: `ScryHandler.kt`.

```kotlin
class ScryHandler : EffectHandler<Effects.Scry> {
    override fun execute(state: GameState, effect: Effects.Scry, context: ExecutionContext): ExecutionResult {
        val player = context.controller
        
        // 1. Look at top N cards
        val topCards = state.library(player).take(effect.amount)
        
        // 2. Create a decision for the player to reorder them
        // (This pauses the engine and asks the player for input)
        return ExecutionResult.PausedForDecision(
            decision = PlayerDecision.ReorderTopCards(
                playerId = player,
                cards = topCards,
                canPutOnBottom = true
            )
        )
    }
}

```

### Step 3: Register the Handler (`mtg-engine`)

Open `EffectHandlerRegistry.kt` and register your new handler so the engine knows how to execute `Effects.Scry`.

### Step 4: Use it in Content (`mtg-sets`)

Now you can use the new effect in card scripts.

**File:** `definitions/theros/cards/Omenspeaker.kt`

```kotlin
val Omenspeaker = cardDef("Omenspeaker") {
    // ... types/stats
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Scry(2)
    }
}

```

---

## 3. Running the Project

### Local Development Environment

1. **Start the Backend API:**
```bash
./gradlew :mtg-api:bootRun

```


* Starts the Spring Boot server on port `8080`.
* Reloads card sets automatically on restart.


2. **Start the Web Client:**
```bash
cd web-client
npm install
npm run dev

```


* Starts the Vite dev server on port `5173`.
* Connects to the local backend WebSocket.



### Building for Production (Docker)

1. **Build the Backend JAR:**
```bash
./gradlew build -x test

```


* Produces `mtg-api/build/libs/mtg-api.jar`.


2. **Build the Frontend Assets:**
```bash
cd web-client
npm run build

```


* Produces static files in `web-client/dist/`.


3. **Run with Docker Compose:**
```bash
docker-compose up --build

```


* **Postgres:** Stores user data and deck lists.
* **App:** Runs the `mtg-api.jar`.
* **Nginx:** Serves the frontend static files and reverse-proxies API calls to the App container.