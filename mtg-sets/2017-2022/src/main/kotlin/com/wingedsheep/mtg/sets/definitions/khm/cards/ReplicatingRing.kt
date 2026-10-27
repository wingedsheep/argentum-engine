package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Replicating Ring — Kaldheim #244.
 *
 * {3} Snow Artifact
 * {T}: Add one mana of any color.
 * At the beginning of your upkeep, put a night counter on this artifact. Then if it has eight or
 * more night counters on it, remove all of them and create eight colorless snow artifact tokens
 * named Replicated Ring with "{T}: Add one mana of any color."
 *
 * The eight-counter check happens only as the upkeep trigger resolves (ruling), so it is the
 * resolution-time [Conditions.SourceCounterCountAtLeast] after the counter goes on.
 */
val ReplicatingRing = card("Replicating Ring") {
    manaCost = "{3}"
    typeLine = "Snow Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "At the beginning of your upkeep, put a night counter on this artifact. Then if it has eight " +
        "or more night counters on it, remove all of them and create eight colorless snow artifact " +
        "tokens named Replicated Ring with \"{T}: Add one mana of any color.\""

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.NIGHT, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.NIGHT, 8),
                then = Effects.RemoveAllCountersOfType(CounterType.NIGHT, EffectTarget.Self) then
                    Effects.CreatePredefinedToken("Replicated Ring", 8),
            )
        description = "At the beginning of your upkeep, put a night counter on this artifact. Then if it " +
            "has eight or more night counters on it, remove all of them and create eight colorless snow " +
            "artifact tokens named Replicated Ring with \"{T}: Add one mana of any color.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b079e285-8431-46aa-bb04-70cac586ed0b.jpg?1783928183"
        ruling("2021-02-05", "The check of whether Replicating Ring has eight or more night counters on it happens only as part of the resolution of the triggered ability. If Replicating Ring picks up its eighth night counter at another time, you won't immediately create Replicated Rings. You'll have to wait until the next time the triggered ability resolves.")
    }
}
