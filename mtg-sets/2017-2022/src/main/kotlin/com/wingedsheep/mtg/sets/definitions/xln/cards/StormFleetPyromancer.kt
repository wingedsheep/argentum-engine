package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Storm Fleet Pyromancer
 * {4}{R}
 * Creature — Human Pirate Wizard
 * 3/2
 * Raid — When this creature enters, if you attacked this turn, this creature deals 2 damage to any target.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the enters trigger (Rule 603.4),
 * the same shape as Storm Fleet Spy.
 */
val StormFleetPyromancer = card("Storm Fleet Pyromancer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate Wizard"
    power = 3
    toughness = 2
    oracleText = "Raid — When this creature enters, if you attacked this turn, this creature deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(2, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab5f5e45-2abb-43e1-aecb-97f0390282a7.jpg?1783935739"
    }
}
