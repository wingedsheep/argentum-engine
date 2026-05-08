package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sazh's Chocobo
 * {G}
 * Creature — Bird
 * 0/1
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 */
val SazhsChocobo = card("Sazh's Chocobo") {
    manaCost = "{G}"
    typeLine = "Creature — Bird"
    power = 0
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Domenico Cava"
        flavorText = "The chick has no name, as voicing the true identity of the animal might trigger ripples of destiny across Cocoon and cause a hurricane in Eden. Either that, or Sazh just hasn't thought of one yet."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dda6b4d0-1b60-46b0-b321-b9ffe15afff4.jpg?1748706509"
    }
}
