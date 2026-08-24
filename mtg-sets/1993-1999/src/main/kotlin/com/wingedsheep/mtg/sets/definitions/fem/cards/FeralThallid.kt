package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feral Thallid
 * {3}{G}{G}{G}
 * Creature — Fungus
 * 6/3
 * At the beginning of your upkeep, put a spore counter on this creature.
 * Remove three spore counters from this creature: Regenerate this creature.
 */
val FeralThallid = card("Feral Thallid") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    oracleText = "At the beginning of your upkeep, put a spore counter on this creature.\n" +
        "Remove three spore counters from this creature: Regenerate this creature."
    power = 6
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.SPORE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a spore counter on this creature."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.SPORE, 3)
        effect = RegenerateEffect(EffectTarget.Self)
        description = "Remove three spore counters from this creature: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Rob Alexander"
        flavorText = "\"Born and bred of fungus, Thallids were nearly impossible to kill.\"\n—*Sarpadian Empires, vol. I*"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e585241e-c647-456d-b3b1-3d48dd78c372.jpg?1783947887"
    }
}
