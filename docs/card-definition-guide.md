# Modular MTG Engine: Comprehensive Card Definition Guide

This document serves as the definitive reference for defining cards within the **Modular MTG Engine**. It follows the **"Configuration over Code"** philosophy: cards are defined as data structures using a type-safe Kotlin DSL, ensuring the core engine remains pure and generic while allowing infinite extensibility.

---

## 1. Core Philosophy

* **SDK (`mtg-sdk`)**: Defines the *vocabulary* (What is a `Cost`? What is `Damage`?). It contains no execution logic.
* **Content (`mtg-sets`)**: Defines the *cards* using the SDK. It has no dependencies on the engine.
* **Engine (`mtg-engine`)**: Executes the logic defined by the Content.

The DSL below belongs to the **Content** module. Each card is typically defined in its own file using the `cardDef` builder.

---

## 2. Lands

Lands are unique because they are often played (not cast) and have implicit mana abilities based on their subtypes.

### Example: `Forest` (Basic Land)

Basic lands typically don't need script logic. The engine infers the `{T}: Add {G}` ability from the `Forest` subtype.

```kotlin
card("Forest") {
    // No mana cost (Land)
    typeLine = "Basic Land — Forest"
    
    // Implicit rules handled by engine:
    // 1. Can be played once per turn.
    // 2. Has "{T}: Add {G}" because of subtype Forest.
    
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "289"
        // Engine auto-fetches image based on set/name unless overridden
    }
}

```

### Example: `Glacial Fortress` (Check Land)

Non-basic lands often have static abilities affecting how they enter the battlefield.

```kotlin
card("Glacial Fortress") {
    typeLine = "Land"
    
    // Static replacement effect: Enters tapped unless condition met
    staticAbility {
        effect = Effects.EntersBattlefieldTappedUnless(
            condition = Conditions.ControlPermanent(
                filter = TargetFilter.Land.withSubtype("Plains", "Island")
            )
        )
    }
    
    // Activated abilities must be explicit for non-basics
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana("{W}")
        isManaAbility = true
    }
    
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana("{U}")
        isManaAbility = true
    }
}

```

### Example: `Evolving Wilds` (Fetch Land)

Sacrifice as a cost to search.

```kotlin
card("Evolving Wilds") {
    typeLine = "Land"
    
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(TargetFilter.Self)
        )
        
        effect = Effects.SearchLibrary(
            target = TargetFilter.BasicLand,
            destination = Zone.BATTLEFIELD,
            tapped = true,
            shuffle = true
        )
    }
}

```

---

## 3. Creatures

### Example: `Grizzly Bears` (Vanilla)

The simplest possible definition.

```kotlin
card("Grizzly Bears") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Bear"
    power = 2
    toughness = 2
    
    metadata {
        flavorText = "We cannot go to the woods today..."
    }
}

```

### Example: `Serra Angel` (Keywords)

Keywords are flags processed by the engine's layers.

```kotlin
card("Serra Angel") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    keywords(Keyword.FLYING, Keyword.VIGILANCE)
}

```

### Example: `Flametongue Kavu` (ETB Trigger)

Triggers use the `triggeredAbility` block.

```kotlin
card("Flametongue Kavu") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Kavu"
    power = 4
    toughness = 2
    
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DealDamage(amount = 4)
        target = TargetFilter.Creature
    }
}

```

### Example: `Thragtusk` (Multiple Triggers)

You can define multiple abilities of the same type.

```kotlin
card("Thragtusk") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 3
    
    // Trigger 1: Life gain
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(5, Target.Controller)
    }
    
    // Trigger 2: Token creation
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.CreateToken("Beast", 3, 3)
    }
}

```

---

## 4. Spells (Sorceries & Instants)

Uses the `spell { ... }` block to define resolution behavior.

### Example: `Divination` (Simple Effect)

```kotlin
card("Divination") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    
    spell {
        effect = Effects.DrawCards(2)
        // Default target is "You" (Controller)
    }
}

```

### Example: `Lightning Bolt` (Targeted)

```kotlin
card("Lightning Bolt") {
    manaCost = "{R}"
    typeLine = "Instant"
    
    spell {
        effect = Effects.DealDamage(3)
        // Validates target selection before casting
        target = TargetFilter.Any // Creature, Player, or Planeswalker
    }
}

```

### Example: `Cryptic Command` (Modal)

Complex choices are handled via the `modes` builder.

```kotlin
card("Cryptic Command") {
    manaCost = "{1}{U}{U}{U}"
    typeLine = "Instant"
    
    spell {
        modes(choose = 2) {
            mode("Counter target spell") {
                effect = Effects.CounterSpell(Target.TargetedSpell)
                requiresTarget = TargetFilter.Spell
            }
            mode("Return target permanent to hand") {
                effect = Effects.ReturnToHand(Target.TargetedPermanent)
                requiresTarget = TargetFilter.Permanent
            }
            mode("Tap all creatures opponents control") {
                effect = Effects.Tap(TargetFilter.Creature.controlledBy(Target.Opponent))
            }
            mode("Draw a card") {
                effect = Effects.DrawCards(1)
            }
        }
    }
}

```

---

## 5. Artifacts & Enchantments

### Example: `Sol Ring` (Mana Rock)

```kotlin
card("Sol Ring") {
    manaCost = "{1}"
    typeLine = "Artifact"
    
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana("{C}{C}")
        isManaAbility = true
    }
}

```

### Example: `Glorious Anthem` (Static Ability)

Static abilities apply continuous effects via the Layer System.

```kotlin
card("Glorious Anthem") {
    manaCost = "{1}{W}{W}"
    typeLine = "Enchantment"
    
    staticAbility {
        // Continuous effect
        effect = Effects.ModifyPowerToughness(1, 1)
        // Filter determines scope
        filter = TargetFilter.Creature.controlledBy(Target.You)
    }
}

```

### Example: `Rancor` (Aura)

Auras combine targeting (spell) with static effects (permanent).

```kotlin
card("Rancor") {
    manaCost = "{G}"
    typeLine = "Enchantment — Aura"
    
    // Defines "Enchant Creature"
    auraTarget = TargetFilter.Creature
    
    // Static buff to attached object
    staticAbility {
        effect = Effects.Composite(
            Effects.ModifyPowerToughness(2, 0),
            Effects.GrantKeyword(Keyword.TRAMPLE)
        )
        filter = TargetFilter.AttachedPermanent
    }
    
    // Recursion trigger
    triggeredAbility {
        trigger = Triggers.PutIntoGraveyard(from = Zone.BATTLEFIELD)
        effect = Effects.ReturnToHand(Target.Self)
    }
}

```

### Example: `Equipment` (Loxodon Warhammer)

Equipment requires an `Equip` ability definition.

```kotlin
card("Loxodon Warhammer") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    
    // Static buff to equipped creature
    staticAbility {
        effect = Effects.Composite(
            Effects.ModifyPowerToughness(3, 0),
            Effects.GrantKeyword(Keyword.TRAMPLE),
            Effects.GrantKeyword(Keyword.LIFELINK)
        )
        filter = TargetFilter.EquippedCreature
    }
    
    // Activated ability keyword
    equipAbility(cost = "{3}")
}

```

---

## 6. Planeswalkers

Planeswalkers use `loyaltyAbility` syntax sugar (wrapping Activated Ability).

### Example: `Liliana of the Veil`

```kotlin
card("Liliana of the Veil") {
    manaCost = "{1}{B}{B}"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 3
    
    // +1: Symmetric discard
    loyaltyAbility(+1) {
        effect = Effects.Discard(1, DiscardMethod.CHOOSE)
        target = TargetFilter.Player.all()
    }
    
    // -2: Edict effect
    loyaltyAbility(-2) {
        effect = Effects.Sacrifice(1, TargetFilter.Creature)
        target = TargetFilter.Player
    }
    
    // -6: Pile separation (Complex custom effect)
    loyaltyAbility(-6) {
        effect = Effects.Custom.SeparatePilesAndSacrifice(
            target = Target.TargetedPlayer
        )
        target = TargetFilter.Player
    }
}

```

---

## 7. Atomic Effects (Pipeline Patterns)

For library manipulation, zone movement, and collection-based mechanics, **always prefer composing from atomic pipeline
effects** rather than creating monolithic effect executors. The engine provides small, reusable primitives that chain
into pipelines via `CompositeEffect`. This keeps the engine generic and makes adding new cards trivial — most library
mechanics need zero new executor code.

### Primitives

| Primitive | What it does |
|-----------|-------------|
| `GatherCardsEffect` | Collects cards into a named in-memory collection (no zone change) |
| `SelectFromCollectionEffect` | Presents a choice to split a collection into "selected" and "remainder" |
| `MoveCollectionEffect` | Physically moves a named collection to a destination zone |
| `RevealUntilEffect` | Reveals cards from library one-by-one until a filter matches |
| `ChooseCreatureTypeEffect` | Pauses for the player to name a creature type |
| `ForEachTargetEffect` | Runs a sub-pipeline once per target |
| `ForEachPlayerEffect` | Runs a sub-pipeline once per player (APNAP order) |

Pipeline steps communicate via **named collections** stored in `EffectContext.storedCollections`. Each `Gather` stores
cards under a name (e.g., `"scried"`), and subsequent `Select`/`Move` steps read from that name.

### Using Pre-Built Pipelines

`EffectPatterns` (exposed via `Effects`) provides factory methods for common mechanics:

```kotlin
// Scry 2 — look at top 2, put any on bottom, rest on top
effect = Effects.Scry(2)

// Surveil 2 — look at top 2, put any in graveyard, rest on top
effect = Effects.Surveil(2)

// Mill 3 — top 3 cards to graveyard
effect = Effects.Mill(3)

// Search library for a basic land, put into hand, shuffle
effect = Effects.SearchLibrary(filter = GameObjectFilter.BasicLand)

// Look at top 7, keep 2, rest to graveyard (Ancestral Memories)
effect = EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)

// Look at top 4, put back in any order (Sage Aven)
effect = EffectPatterns.lookAtTopAndReorder(4)

// Reveal until nonland, deal damage equal to its mana value (Erratic Explosion)
effect = EffectPatterns.revealUntilNonlandDealDamage(EffectTarget.ContextTarget(0))

// Wheel — each player shuffles hand into library, then draws that many (Winds of Change)
effect = EffectPatterns.wheelEffect(Player.Each)

// Reveal top 5, opponent chooses a creature, it goes to battlefield, rest to graveyard (Animal Magnetism)
effect = EffectPatterns.revealAndOpponentChooses(count = 5, filter = GameObjectFilter.Creature)

// Reveal until creature of chosen type, put onto battlefield, shuffle rest (Riptide Shapeshifter)
effect = EffectPatterns.revealUntilCreatureTypeToBattlefield()
```

### Full Pipeline Reference

| `EffectPatterns` Method | Pipeline |
|------------------------|----------|
| `scry(n)` | Gather(top n) → Select(up to n for bottom) → Move(bottom) → Move(top) |
| `surveil(n)` | Gather(top n) → Select(up to n for graveyard) → Move(graveyard) → Move(top) |
| `mill(n)` | Gather(top n) → Move(graveyard) |
| `searchLibrary(filter, ...)` | Gather(library, filter) → Select → Move(destination) → Shuffle |
| `lookAtTopAndKeep(n, keep)` | Gather(top n) → Select(exactly keep) → Move(hand) → Move(rest to graveyard) |
| `lookAtTopAndReorder(n)` | Gather(top n) → Move(ControllerChooses order → top) |
| `lookAtTopXAndPutOntoBattlefield(...)` | Gather → Select → Move(battlefield) → Move(rest) |
| `revealUntilNonlandDealDamage(target)` | RevealUntil(nonland) → DealDamage(mana value) → Move(bottom) |
| `revealUntilNonlandModifyStats()` | RevealUntil(nonland) → ModifyStats(+mana value/+0) → Move(bottom) |
| `revealUntilCreatureTypeToBattlefield()` | ChooseType → RevealUntil(match) → Move(battlefield) → Shuffle |
| `revealAndOpponentChooses(n, filter)` | Gather(revealed, top n) → Select(opponent) → Move(battlefield) → Move(rest to graveyard) |
| `wheelEffect(players)` | ForEachPlayer[ Gather(hand) → Move(shuffled into library) → Draw(hand count) ] |

### When to Create a New Executor

Only create a new `EffectExecutor` when the mechanic truly needs new logic that cannot be composed from existing
primitives. Examples of mechanics that **do** need custom executors: dealing damage, gaining life, creating tokens,
modifying stats, countering spells. Examples that **don't** need them: scry, surveil, mill, search library, reveal-and-choose — these are all pipelines.

---

## 8. Advanced Sequencing

Handling "Then", "If you do", and "When you do".

### A. Sequencing (`Effects.Composite`)

**Card:** *Compulsive Research* ("Draw three cards. Then discard two cards...")

```kotlin
spell {
    effect = Effects.Composite(
        Effects.DrawCards(3),
        Effects.Discard(2)
    )
}

```

### B. Dependency (`Effects.ReflexiveTrigger`)

**Card:** *Heart-Piercer Manticore* ("When it enters, you may sacrifice another creature. **When you do**, deal damage...")

This is critical because the second part uses the stack *after* the sacrifice happens.

```kotlin
triggeredAbility {
    trigger = Triggers.EntersBattlefield
    
    effect = Effects.ReflexiveTrigger(
        // The optional cost
        action = Effects.Sacrifice(TargetFilter.Creature.other()),
        optional = true,
        
        // The resulting ability that goes on stack
        reflexiveEffect = Effects.DealDamage(
            amount = Values.SacrificedCreaturePower,
            target = TargetFilter.Any
        )
    )
}

```

### C. Conditional Branching (`Effects.Branch`)

**Card:** *Coiling Oracle* ("Reveal top card. If land, put onto battlefield. Otherwise, put into hand.")

```kotlin
triggeredAbility {
    trigger = Triggers.EntersBattlefield
    
    effect = Effects.RevealTopCard(
        then = Effects.Branch(
            condition = Conditions.TopCardMatches(TargetFilter.Land),
            ifTrue = Effects.PutTopCardOnBattlefield,
            ifFalse = Effects.DrawCards(1) // "Put into hand"
        )
    )
}

```

---

## 9. Metadata Management

Separating visual data from game rules prevents bloat and allows the frontend to be updated independently of the engine.

```kotlin
card("Grizzly Bears") {
    // --- ENGINE DATA (Required) ---
    manaCost = "{1}{G}"
    typeLine = "Creature — Bear"
    power = 2
    toughness = 2
    
    // --- FRONTEND DATA (Optional) ---
    metadata {
        // Used for deckbuilder filters
        rarity = Rarity.COMMON
        
        // Used for rendering
        artist = "D. J. Cleland-Hura"
        flavorText = "We cannot go to the woods today..."
        
        // Custom image override (defaults to Scryfall API based on Set/Name)
        imageUri = "https://my-server.com/images/custom/grizzly-bears-alt.jpg"
        
        // Overrides auto-generated rule text
        oracleTextOverride = "This bear is vanilla."
    }
}

```
