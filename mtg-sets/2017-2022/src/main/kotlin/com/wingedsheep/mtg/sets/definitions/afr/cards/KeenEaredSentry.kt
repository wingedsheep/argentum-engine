package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.RestrictVenturesPerTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Keen-Eared Sentry
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 * You have hexproof. (You can't be the target of spells or abilities your opponents control.)
 * Each opponent can't venture into the dungeon more than once each turn.
 */
val KeenEaredSentry = card("Keen-Eared Sentry") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "You have hexproof. (You can't be the target of spells or abilities your opponents control.)\n" +
        "Each opponent can't venture into the dungeon more than once each turn."
    power = 2
    toughness = 1

    staticAbility {
        ability = GrantHexproofToController
    }

    staticAbility {
        ability = RestrictVenturesPerTurn(affected = Player.EachOpponent, maxPerTurn = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Viko Menezes"
        flavorText = "\"You're not the first to underestimate me.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de169d64-8242-4f57-980e-47f901fd57ab.jpg?1783926531"
    }
}
