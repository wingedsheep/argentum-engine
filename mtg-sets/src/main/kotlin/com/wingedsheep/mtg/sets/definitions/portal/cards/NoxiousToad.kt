package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachOpponentDiscardsEffect

/**
 * Noxious Toad
 * {2}{B}
 * Creature — Frog
 * 1/1
 * When Noxious Toad dies, each opponent discards a card.
 */
val NoxiousToad = card("Noxious Toad") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Frog"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = EachOpponentDiscardsEffect(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Ron Spencer"
        flavorText = "Even in death, its poison lingers."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e5b0e7-8c2d-4e3a-9f4b-5c6d7e8f9a0b.jpg"
    }
}
