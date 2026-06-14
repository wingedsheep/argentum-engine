package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The One Ring
 * {4}
 * Legendary Artifact
 *
 * Indestructible
 * When The One Ring enters, if you cast it, you gain protection from everything until your next turn.
 * At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.
 * {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on The One Ring.
 *
 * Engine notes (Gap 8 — player-level protection):
 * - "You gain protection from everything until your next turn" is the player counterpart of the
 *   creature protection statics. Modeled via `Effects.GrantPlayerProtection()` (defaults to
 *   ProtectionScope.Everything / Duration.UntilYourNextTurn / the controller), which adds a
 *   `PlayerProtectionComponent`. The targeting validator + target enumerator + damage executor
 *   all consult `PlayerProtectionRules`, so while protected the controller can't be targeted by,
 *   or dealt damage from, any source (CR 702.16). Cleared after the untap step of their next turn.
 * - The ETB is gated on `Conditions.WasCast` ("if you cast it") — putting The One Ring onto the
 *   battlefield by another effect grants no protection (CR 603.4 intervening-if).
 * - The {T} ability adds the burden counter first, then draws reading the *new* count (sequential
 *   resolution, CR 608.2c): `AddCounters` then `DrawCards(countersOnSelf(burden))`.
 * - The burden counter has no rules meaning of its own; it only feeds the upkeep life-loss and the
 *   draw count, and grows the upkeep tax each activation.
 */
val TheOneRing = card("The One Ring") {
    manaCost = "{4}"
    typeLine = "Legendary Artifact"
    oracleText = "Indestructible\n" +
        "When The One Ring enters, if you cast it, you gain protection from everything until your next turn.\n" +
        "At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.\n" +
        "{T}: Put a burden counter on The One Ring, then draw a card for each burden counter on The One Ring."

    keywords(Keyword.INDESTRUCTIBLE)

    // When The One Ring enters, if you cast it, you gain protection from everything until your next turn.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = Effects.GrantPlayerProtection()
    }

    // At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.LoseLife(
            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.BURDEN)),
            EffectTarget.Controller
        )
    }

    // {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on it.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            listOf(
                Effects.AddCounters(Counters.BURDEN, 1, EffectTarget.Self),
                Effects.DrawCards(DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.BURDEN)))
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "246"
        artist = "Veli Nyström"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5806e68-1054-458e-866d-1f2470f682b2.jpg?1763472900"
    }
}
