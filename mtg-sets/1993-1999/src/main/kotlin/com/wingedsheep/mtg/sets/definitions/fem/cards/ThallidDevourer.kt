package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thallid Devourer
 * {1}{G}{G}
 * Creature — Fungus
 * 2/2
 * At the beginning of your upkeep, put a spore counter on this creature.
 * Remove three spore counters from this creature: Create a 1/1 green Saproling creature token.
 * Sacrifice a Saproling: This creature gets +1/+2 until end of turn.
 */
val ThallidDevourer = card("Thallid Devourer") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    oracleText = "At the beginning of your upkeep, put a spore counter on this creature.\n" +
        "Remove three spore counters from this creature: Create a 1/1 green Saproling creature token.\n" +
        "Sacrifice a Saproling: This creature gets +1/+2 until end of turn."
    power = 2
    toughness = 2

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

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.SAPROLING))
        effect = Effects.ModifyStats(1, 2, EffectTarget.Self)
        description = "Sacrifice a Saproling: This creature gets +1/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa533845-4c4b-4072-aa39-8e56ce7ec325.jpg?1783947884"
    }
}
