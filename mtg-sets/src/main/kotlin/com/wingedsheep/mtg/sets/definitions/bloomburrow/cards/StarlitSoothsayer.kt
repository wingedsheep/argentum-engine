package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Starlit Soothsayer {2}{B}
 * Creature — Bat Cleric
 * 2/2
 *
 * Flying
 * At the beginning of your end step, if you gained or lost life this turn, surveil 1.
 */
val StarlitSoothsayer = card("Starlit Soothsayer") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Bat Cleric"
    oracleText = "Flying\nAt the beginning of your end step, if you gained or lost life this turn, surveil 1."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedOrLostLifeThisTurn
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Kaitlyn McCulley"
        flavorText = "\"The Lunar Paean is upon us. Let us turn to our ancestors for guidance, and look to our children for hope.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/184c1eca-2991-438f-b5d2-cd2529b9c9b4.jpg?1721426524"
    }
}
