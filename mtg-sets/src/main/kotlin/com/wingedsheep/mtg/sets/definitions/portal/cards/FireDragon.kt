package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDynamicDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Fire Dragon
 * {6}{R}{R}{R}
 * Creature — Dragon
 * 6/6
 * Flying
 * When Fire Dragon enters the battlefield, it deals damage to target creature
 * equal to the number of Mountains you control.
 */
val FireDragon = card("Fire Dragon") {
    manaCost = "{6}{R}{R}{R}"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = OnEnterBattlefield()
        target = TargetCreature()
        effect = DealDynamicDamageEffect(
            amount = DynamicAmount.LandsWithSubtypeYouControl(Subtype.MOUNTAIN),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Edward Beard, Jr."
        flavorText = "Its breath carries the fury of the mountains themselves."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5a6b7c8-d9e0-1f2a-3b4c-5d6e7f8a9b0c.jpg"
    }
}
