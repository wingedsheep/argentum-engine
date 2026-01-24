package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Fire Imp
 * {2}{R}
 * Creature — Imp
 * 2/1
 * When Fire Imp enters the battlefield, it deals 2 damage to target creature.
 */
val FireImp = card("Fire Imp") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Imp"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = OnEnterBattlefield()
        target = TargetCreature()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Pete Venters"
        flavorText = "Small but deadly."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06a7b8c9-d0e1-f2a3-b4c5-d6e7f8a9b0c1.jpg"
    }
}
