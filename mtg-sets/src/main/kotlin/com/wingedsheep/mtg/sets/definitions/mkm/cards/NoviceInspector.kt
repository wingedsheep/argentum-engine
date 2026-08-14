package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Novice Inspector
 * {W}
 * Creature — Human Detective
 * 1/2
 * When this creature enters, investigate.
 */
val NoviceInspector = card("Novice Inspector") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Detective"
    oracleText = "When this creature enters, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Fajareka Setiawan"
        flavorText = "\"If the perfect evidence appears at your feet, your first task is to rule out " +
            "misdirection.\"\n—The Ravnican Agency of Magicological Investigations handbook"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ad38866-fc5f-4f62-89c1-afc0f50765aa.jpg?1783912920"
    }
}
