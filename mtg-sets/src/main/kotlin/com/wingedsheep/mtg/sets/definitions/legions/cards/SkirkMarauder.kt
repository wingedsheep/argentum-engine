package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect

/**
 * Skirk Marauder
 * {1}{R}
 * Creature — Goblin
 * 2/1
 * Morph {2}{R}
 * When Skirk Marauder is turned face up, it deals 2 damage to any target.
 */
val SkirkMarauder = card("Skirk Marauder") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "Morph {2}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Skirk Marauder is turned face up, it deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("target", Targets.Any)
        effect = DealDamageEffect(2, t)
    }

    morph = "{2}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbd2ff12-c6f7-4986-801f-225ad6f59278.jpg?1562932762"
    }
}
