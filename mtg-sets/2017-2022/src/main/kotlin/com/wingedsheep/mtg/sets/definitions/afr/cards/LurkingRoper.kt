package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lurking Roper
 * {2}{G}
 * Creature — Horror
 * 4/5
 * This creature doesn't untap during your untap step.
 * Whenever you gain life, untap this creature.
 */
val LurkingRoper = card("Lurking Roper") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Horror"
    oracleText = "This creature doesn't untap during your untap step.\nWhenever you gain life, untap this creature."
    power = 4
    toughness = 5
    flags(AbilityFlag.DOESNT_UNTAP)
    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.Untap(EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Andrew Mar"
        flavorText = "\"You see, stalac-*tites* grow down from the ceiling, while stalag-*mites* reach—AAHHHHHHHHH—\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cce25d7-3f22-48ce-9428-184e661696b9.jpg?1783926459"
    }
}
