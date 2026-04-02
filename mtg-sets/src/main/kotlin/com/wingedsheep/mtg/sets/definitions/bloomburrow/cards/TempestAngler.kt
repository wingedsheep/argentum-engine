package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tempest Angler
 * {1}{U/R}{U/R}
 * Creature — Otter Wizard
 * 2/2
 *
 * Whenever you cast a noncreature spell, put a +1/+1 counter on this creature.
 */
val TempestAngler = card("Tempest Angler") {
    manaCost = "{1}{U/R}{U/R}"
    typeLine = "Creature — Otter Wizard"
    oracleText = "Whenever you cast a noncreature spell, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Raluca Marinescu"
        flavorText = "\"Castin' lines or castin' spells, ain't no one better than me.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/850daae4-f0b7-4604-95e7-ad044ec165c3.jpg?1721427207"
    }
}
