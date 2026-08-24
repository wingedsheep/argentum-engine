package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thorn Thallid
 * {1}{G}{G}
 * Creature — Fungus
 * 2/2
 * At the beginning of your upkeep, put a spore counter on this creature.
 * Remove three spore counters from this creature: It deals 1 damage to any target.
 */
val ThornThallid = card("Thorn Thallid") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    oracleText = "At the beginning of your upkeep, put a spore counter on this creature.\n" +
        "Remove three spore counters from this creature: It deals 1 damage to any target."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.SPORE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a spore counter on this creature."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.SPORE, 3)
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "Remove three spore counters from this creature: It deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80a"
        artist = "Daniel Gelon"
        flavorText = "\"The danger in cultivating massive plants caught the Elves by surprise.\"\n—*Sarpadian Empires, vol. III*"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16e61c00-3e94-4f6f-8515-65b430829e91.jpg?1783947882"
    }
}
