package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enchanted Carriage
 * {5}
 * Artifact — Vehicle
 * 4/4
 *
 * When this Vehicle enters, create two 1/1 white Mouse creature tokens.
 * Crew 2
 */
val EnchantedCarriage = card("Enchanted Carriage") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    oracleText = "When this Vehicle enters, create two 1/1 white Mouse creature tokens.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Mouse"),
            count = 2,
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0057c2ae-ea4f-404a-ab95-f3979efd1b3b.jpg?1783932587"
    }
}
