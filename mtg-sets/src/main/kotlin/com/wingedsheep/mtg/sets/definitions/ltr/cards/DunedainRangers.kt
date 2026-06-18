package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dúnedain Rangers
 * {3}{G}
 * Creature — Human Ranger
 * 4/4
 *
 * Landfall — Whenever a land you control enters, if you don't control a Ring-bearer, the Ring tempts you.
 *
 * Gap 18 (player-level Ring-bearer condition): adds `StatePredicate.IsRingBearer` +
 * `GameObjectFilter.Creature.ringBearer()`, consumed via the existing
 * `Conditions.YouControl(filter, negate = true)` as the landfall trigger's intervening-if.
 */
val DunedainRangers = card("Dúnedain Rangers") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger"
    power = 4
    toughness = 4
    oracleText = "Landfall — Whenever a land you control enters, if you don't control a Ring-bearer, " +
        "the Ring tempts you."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        triggerCondition = Conditions.YouControl(GameObjectFilter.Creature.ringBearer(), negate = true)
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Eric Wilkerson"
        flavorText = "\"Aragorn has need of his kindred. Let the Dúnedain go to him in Rohan!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/630e1e36-2f5d-44d4-9ff2-19ae75295016.jpg?1687694686"
    }
}
