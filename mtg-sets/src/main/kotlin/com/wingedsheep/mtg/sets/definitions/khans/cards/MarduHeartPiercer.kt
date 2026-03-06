package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect

/**
 * Mardu Heart-Piercer
 * {3}{R}
 * Creature — Human Archer
 * 2/3
 * Raid — When Mardu Heart-Piercer enters, if you attacked this turn,
 * Mardu Heart-Piercer deals 2 damage to any target.
 */
val MarduHeartPiercer = card("Mardu Heart-Piercer") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Human Archer"
    power = 2
    toughness = 3
    oracleText = "Raid — When Mardu Heart-Piercer enters, if you attacked this turn, Mardu Heart-Piercer deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = YouAttackedThisTurn
        val t = target("any target", Targets.Any)
        effect = DealDamageEffect(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Karl Kopinski"
        flavorText = "\"Those who have never ridden before the wind do not know the true joy of war.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d17b6eee-da22-48aa-ba8a-cbd1a3389bcb.jpg?1562793937"
    }
}
