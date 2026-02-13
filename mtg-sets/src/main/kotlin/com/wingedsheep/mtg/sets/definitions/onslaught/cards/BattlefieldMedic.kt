package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.PreventNextDamageEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Battlefield Medic
 * {1}{W}
 * Creature — Human Cleric
 * 1/1
 * {T}: Prevent the next X damage that would be dealt to target creature this turn,
 * where X is the number of Clerics on the battlefield.
 */
val BattlefieldMedic = card("Battlefield Medic") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{T}: Prevent the next X damage that would be dealt to target creature this turn, where X is the number of Clerics on the battlefield."

    activatedAbility {
        cost = Costs.Tap
        target = TargetPermanent(filter = TargetFilter.Creature)
        effect = PreventNextDamageEffect(
            amount = DynamicAmounts.creaturesWithSubtype(Subtype("Cleric")),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Kev Walker"
        flavorText = "\"Death never stops to rest. Neither can we.\""
        imageUri = "https://cards.scryfall.io/large/front/9/c/9c444503-42a8-4952-819b-bbca89b06abc.jpg?1562931867"
    }
}
