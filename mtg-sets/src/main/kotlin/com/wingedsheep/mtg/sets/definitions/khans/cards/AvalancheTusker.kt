package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Avalanche Tusker
 * {2}{G}{U}{R}
 * Creature — Elephant Warrior
 * 6/4
 * Whenever Avalanche Tusker attacks, target creature defending player controls blocks it this combat if able.
 */
val AvalancheTusker = card("Avalanche Tusker") {
    manaCost = "{2}{G}{U}{R}"
    typeLine = "Creature — Elephant Warrior"
    power = 6
    toughness = 4
    oracleText = "Whenever Avalanche Tusker attacks, target creature defending player controls blocks it this combat if able."

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target("creature defending player controls", Targets.CreatureOpponentControls)
        effect = Effects.ForceBlock(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Matt Stewart"
        flavorText = "\"Hold the high ground, then bring it to your enemy.\" —Surrak, khan of the Temur"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/408e3f12-53d5-45f4-8ccc-6c00f8a9c6fe.jpg?1562785458"
    }
}
