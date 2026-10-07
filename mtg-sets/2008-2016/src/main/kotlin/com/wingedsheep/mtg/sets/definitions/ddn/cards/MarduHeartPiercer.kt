package com.wingedsheep.mtg.sets.definitions.ddn.cards

import com.wingedsheep.sdk.dsl.Conditions

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

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
    colorIdentity = "R"
    typeLine = "Creature — Human Archer"
    power = 2
    toughness = 3
    oracleText = "Raid — When Mardu Heart-Piercer enters, if you attacked this turn, Mardu Heart-Piercer deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Karl Kopinski"
        flavorText = "\"Those who have never ridden before the wind do not know the true joy of war.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/019be6df-da52-4cb0-81ee-a684e6f73e43.jpg?1783939124"
    }
}
