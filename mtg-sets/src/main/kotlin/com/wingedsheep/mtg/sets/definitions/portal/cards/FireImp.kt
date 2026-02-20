package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Triggers

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
        trigger = Triggers.EntersBattlefield
        target = TargetCreature()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Pete Venters"
        flavorText = "Small but deadly."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7edaf3-7941-4085-bdbc-e5c9832b6444.jpg"
    }
}
