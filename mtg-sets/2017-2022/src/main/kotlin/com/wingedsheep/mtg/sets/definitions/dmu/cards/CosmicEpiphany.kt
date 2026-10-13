package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cosmic Epiphany
 * {4}{U}{U}
 * Sorcery
 * Draw cards equal to the number of instant and sorcery cards in your graveyard.
 *
 * Counted while the spell is still resolving on the stack, so it never counts itself.
 */
val CosmicEpiphany = card("Cosmic Epiphany") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the number of instant and sorcery cards in your graveyard."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "283"
        artist = "Eli Minaya"
        flavorText = "\"All knowledge, past and future, is etched upon reality. Every answer we seek is out there, hidden only by the limits of our minds.\"\n—Jodah"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79718dbe-d5d6-4f24-b514-5c392963f021.jpg?1783921245"
        ruling(
            "2022-09-09",
            "Cosmic Epiphany is still resolving at the time that the instant and sorcery cards in your graveyard are counted, so it isn't in your graveyard yet and won't be counted."
        )
    }
}
