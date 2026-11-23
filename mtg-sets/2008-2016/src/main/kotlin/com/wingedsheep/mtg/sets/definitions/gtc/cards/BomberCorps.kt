package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Bomber Corps
 * {1}{R}
 * Creature — Human Soldier
 * 1/2
 * Battalion — Whenever this creature and at least two other creatures attack, this creature
 * deals 1 damage to any target.
 */
val BomberCorps = card("Bomber Corps") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Chase Stone"
        flavorText = "\"Subtlety is for lawmages and spies.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b1675f2-e950-4f3c-9dd3-29ead615ff23.jpg?1783940126"
    }
}
