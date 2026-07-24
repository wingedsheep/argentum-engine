package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Millennium Calendar
 * {1}
 * Legendary Artifact
 *
 * Whenever you untap one or more permanents during your untap step, put that many time counters
 * on The Millennium Calendar.
 * {2}, {T}: Double the number of time counters on The Millennium Calendar.
 * When there are 1,000 or more time counters on The Millennium Calendar, sacrifice it and each
 * opponent loses 1,000 life.
 *
 * Three abilities over the shared TIME counter type (reused from suspend/vanishing):
 *  - The untap payoff is a **batch** trigger ([Triggers.OneOrMoreBecomeUntapped], CR 603.2c): the
 *    untap step untaps all your permanents at once but the ability fires a single time, with the
 *    untapped permanents exposed as the trigger's captured collection so "that many" is read with
 *    `DynamicAmount.DistinctEntitiesInCollections(TRIGGER_CAPTURED_COLLECTION)`. The "during your
 *    untap step" scoping is intrinsic to the batch untap trigger (`TriggerDetector` fires it only
 *    for the active player's untap-step untaps), so no `triggerCondition` is needed.
 *  - The doubler is [Effects.DoubleCounters] (reads the current count and adds that many; doubling
 *    zero stays zero).
 *  - The 1,000-counter payoff is a CR 603.8 **state-triggered** ability — it watches the counter
 *    count itself rather than an event, fires once when the threshold is met (e.g. right after a
 *    doubling from ≥500), and self-sacrifices so the latch never re-fires. "Each opponent loses
 *    1,000 life" is life loss (not damage).
 */
val TheMillenniumCalendar = card("The Millennium Calendar") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever you untap one or more permanents during your untap step, put that many " +
        "time counters on The Millennium Calendar.\n" +
        "{2}, {T}: Double the number of time counters on The Millennium Calendar.\n" +
        "When there are 1,000 or more time counters on The Millennium Calendar, sacrifice it and " +
        "each opponent loses 1,000 life."

    // Whenever you untap one or more permanents during your untap step, put that many time counters.
    triggeredAbility {
        trigger = Triggers.OneOrMoreBecomeUntapped(GameObjectFilter.Permanent.youControl())
        effect = Effects.AddDynamicCounters(
            Counters.TIME,
            DynamicAmount.DistinctEntitiesInCollections(
                listOf(IterationSpace.TRIGGER_CAPTURED_COLLECTION)
            ),
            EffectTarget.Self
        )
    }

    // {2}, {T}: Double the number of time counters on The Millennium Calendar.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.DoubleCounters(Counters.TIME, EffectTarget.Self)
        description = "Double the number of time counters on The Millennium Calendar."
    }

    // When there are 1,000 or more time counters on ~, sacrifice it and each opponent loses 1,000 life.
    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.TIME, 1000)
        effect = Effects.Composite(
            Effects.SacrificeTarget(EffectTarget.Self),
            Effects.LoseLife(1000, EffectTarget.PlayerRef(Player.EachOpponent))
        )
        description = "When there are 1,000 or more time counters on The Millennium Calendar, " +
            "sacrifice it and each opponent loses 1,000 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "257"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deabba7f-05ef-41cf-ae3a-d950d051cf1e.jpg?1782694405"
    }
}
