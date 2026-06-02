# Developer Guide: How to Extend Argentum

Step-by-step instructions for the most common extension tasks: adding cards, adding sets, adding
mechanics, and running the stack locally.

For the *full* card-DSL catalog (every effect, trigger, condition, filter, cost, keyword, dynamic
amount, etc.), see [`card-sdk-language-reference.md`](card-sdk-language-reference.md). For the
architectural reasoning behind the engine, see [`architecture-principles.md`](architecture-principles.md).

> When adding a card from a backlog file (e.g. `backlog/sets/scourge/cards.md`), use the `add-card`
> skill — it handles Scryfall lookup, oracle errata, reprint detection, set registration, and a
> scenario test in one pass.

---

## 1. Adding a New Card

Cards live in **`mtg-sets`**, one file per card, under the set's `cards/` package:

```text
mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/
  └── lgn/
      ├── LegionsSet.kt
      └── cards/
          ├── AkromaAngelOfWrath.kt
          └── SkirkMarauder.kt
```

Each card is a top-level `val` produced by the `card("Name") { … }` DSL builder. The set's `cards`
list is populated automatically by `CardDiscovery.findIn(...)` — you do **not** need to register
the card manually anywhere.

### Example: a simple instant

```kotlin
package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SparkSpray = card("Spark Spray") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spark Spray deals 1 damage to any target."

    spell {
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        flavorText = "It's the only kind of shower goblins will tolerate."
    }
}
```

### Example: a creature with a triggered ability

```kotlin
val BellowingCrier = card("Bellowing Crier") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "When Bellowing Crier enters the battlefield, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot()
    }

    metadata { rarity = Rarity.COMMON }
}
```

### Key conventions

- **Use the facades**, not raw constructors: `Effects.*`, the pattern objects (`LibraryPatterns.*`,
  `HandPatterns.*`, `GroupPatterns.*`, `ExilePatterns.*`, `CreatureTypePatterns.*`, `MiscPatterns.*`), `Triggers.*`,
  `Costs.*`, `Conditions.*`, `Filters.*`. Raw data-class construction bypasses the curated API and
  ages badly when shapes change.
- **Targets**: declare each target inside `spell { … }` / `triggeredAbility { … }` with
  `val t = target("label", AnyTarget())`, then pass `t` to the effect (or reference it as
  `EffectTarget.ContextTarget(0)` in nested/modal shapes).
- **Reprints**: if the card already exists as a `CardDefinition` in an earlier set, do **not**
  duplicate the file. Add a `Printing` row in the new set's `Reprints.kt` and wire it through
  `MtgSet.printings`. The `add-card` skill handles this automatically.
- **All scripting types must be `@Serializable`** — `Effect`, `Trigger`, `StaticAbility`,
  `TargetRequirement`, `Condition`, etc. `CardDefinition` is serialized for transport, and a missing
  annotation fails at runtime.

---

## 2. Adding a New Card Set

### Step 1: Create the package

```text
mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/
  └── mns/
      ├── MyNewSet.kt
      └── cards/
          └── (card files go here)
```

### Step 2: Write the set object

The set is an `object` implementing the `MtgSet` interface from
`com.wingedsheep.sdk.model.MtgSet`. Cards are auto-discovered from the `cards/` sub-package via
reflection.

```kotlin
package com.wingedsheep.mtg.sets.definitions.mns

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

object MyNewSet : MtgSet {
    override val code = "MNS"
    override val displayName = "My New Set"
    override val releaseDate = "2026-05-17"
    override val block: String? = null
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mns.cards"
}
```

Optional overrides commonly used by real sets:

- `basicLandsFallback = OnslaughtSet` — point at another set's basics when yours has none.
- `boosterStrategy = …` — override the default 11C / 3U / 1R(or mythic) pack for custom slots.
- `printings: List<Printing>` — register reprints whose canonical `CardDefinition` lives in an
  earlier set.

### Step 3: Register in `MtgSetCatalog`

Open `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/MtgSetCatalog.kt` and add your set object to
the `all` list:

```kotlin
object MtgSetCatalog {
    val all: List<MtgSet> = listOf(
        // … existing sets …
        MyNewSet,
    )
}
```

That's the only registration step. The engine loads every set in `MtgSetCatalog.all` at startup.

### Step 4: Verify

```bash
just server                # boots game-server on :8080
scripts/card-status --set MNS   # confirms which cards are recognised
```

---

## 3. Adding a New Mechanic

### Preferred: compose from atomic effects

Most library- and zone-manipulation mechanics can be built from existing primitives without writing
new executor code. The engine provides composable building blocks that chain into pipelines:

| Primitive | Purpose |
|-----------|---------|
| `GatherCardsEffect` | Collect cards into a named collection (no zone change) |
| `SelectFromCollectionEffect` | Player chooses cards from a collection |
| `MoveCollectionEffect` | Move a collection to a zone (hand, graveyard, library top/bottom, battlefield) |
| `RevealUntilEffect` | Reveal from library until a filter matches |
| `ForEachPlayerEffect` / `ForEachTargetEffect` | Iterate a sub-pipeline per player or target |

Many mechanics are already available as pre-built compositions in the domain pattern objects
(`LibraryPatterns`, `HandPatterns`, `GroupPatterns`, `ExilePatterns`, `CreatureTypePatterns`,
`MiscPatterns`):

```kotlin
LibraryPatterns.scry(2)          // Gather → Select → Move(bottom) → Move(top)
LibraryPatterns.surveil(2)       // Gather → Select → Move(graveyard) → Move(top)
LibraryPatterns.mill(3)          // Gather → Move(graveyard)
LibraryPatterns.searchLibrary(filter = GameObjectFilter.BasicLand)

LibraryPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)   // Ancestral Memories
HandPatterns.wheelEffect(Player.Each)                     // Winds of Change
LibraryPatterns.revealUntilNonlandDealDamage(target)         // Erratic Explosion
```

If your mechanic fits the gather/select/move shape, add a new factory method to the relevant
pattern object (e.g. `LibraryPatterns.kt`) and (if it deserves a top-level facade) expose it
through `Effects.kt`. See the language
reference §5 for the full pipeline catalog.

Chain effects with `.then(...)`:

```kotlin
spell {
    effect = LibraryPatterns.scry(1).then(Effects.DrawCards(1))   // Opt
}
```

### Fallback: a new `EffectExecutor`

Only write a new executor when the mechanic needs genuinely new logic (dealing damage, creating
tokens, countering spells, etc.). For these, you touch all three backend modules:

1. **`mtg-sdk`** — add a `@Serializable` data class implementing `Effect` under
   `com.wingedsheep.sdk.scripting.effects/`.
2. **`rules-engine`** — implement an `EffectExecutor` under `handlers/effects/` and register it
   in the appropriate `ExecutorModule`.
3. **`mtg-sets`** — expose it through `Effects.kt` (the facade) and use it in card scripts.

When you add anything to the SDK, update
[`card-sdk-language-reference.md`](card-sdk-language-reference.md) in the same change — it's the
canonical catalog and rots fast otherwise.

---

## 4. Running the Project

### Local development

The `justfile` is the primary entry point. Direct gradle / npm commands work too if you prefer.

```bash
just build                          # build everything
just server                         # start game-server on :8080 (Spring Boot)
just client                         # start web-client dev server on :5173 (Vite)

just test                           # all tests
just test-rules                     # rules-engine only
just test-server                    # game-server only
just test-class CreatureStatsTest   # a single class (matches across modules)
```

The game-server reloads card sets on restart. The web-client connects to the local backend
WebSocket automatically.

### Production build

```bash
./gradlew :game-server:build -x test   # produces game-server/build/libs/game-server.jar
cd web-client && npm run build         # produces web-client/dist/

docker-compose up --build              # postgres + app (game-server.jar) + nginx
```

`docker-compose.yml` wires Nginx in front of the app container — it serves the frontend bundle
statically and reverse-proxies API and WebSocket traffic to the Spring Boot service.
