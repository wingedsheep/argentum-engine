package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thallid
 * {G}
 * Creature — Fungus
 * 1/1
 * At the beginning of your upkeep, put a spore counter on this creature.
 * Remove three spore counters from this creature: Create a 1/1 green Saproling creature token.
 *
 * The archetype of the set's spore engine — an upkeep trigger that accrues counters and a
 * remove-three activation that cashes them in. The Saproling art is registered on
 * [com.wingedsheep.mtg.sets.definitions.fem.FallenEmpiresSet].
 */
val Thallid = card("Thallid") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    oracleText = "At the beginning of your upkeep, put a spore counter on this creature.\n" +
        "Remove three spore counters from this creature: Create a 1/1 green Saproling creature token."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.SPORE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a spore counter on this creature."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.SPORE, 3)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
        description = "Remove three spore counters from this creature: Create a 1/1 green Saproling creature token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74a"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4caaf31b-86a9-485b-8da7-d5b526ed1233.jpg?1783947885"
    }
}
