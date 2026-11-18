package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grindclock
 * {2}
 * Artifact
 *
 * {T}: Put a charge counter on this artifact.
 * {T}: Target player mills X cards, where X is the number of charge counters on this artifact.
 *
 * X reads the source's counters on resolution; if Grindclock has left the battlefield by then, the
 * evaluator's last-known-information fallback for `EffectTarget.Self` uses the counters it left with.
 */
val Grindclock = card("Grindclock") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Put a charge counter on this artifact.\n" +
        "{T}: Target player mills X cards, where X is the number of charge counters on this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{T}: Put a charge counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.countersOnSelf(CounterType.CHARGE), player)
        description = "{T}: Target player mills X cards, where X is the number of charge counters on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Nils Hamm"
        flavorText = "Pray you never hear it chime."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6df2e7f-e46e-4808-8125-42a3aa66377c.jpg?1783941708"
        ruling("2014-07-18", "If Grindclock isn't on the battlefield when its last ability resolves, use the number of charge counters on it when it left the battlefield to determine how many cards to put into the graveyard.")
    }
}
