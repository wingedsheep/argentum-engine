package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Conclave Cavalier
 * {G}{G}{W}{W}
 * Creature — Centaur Knight
 * 4/4
 * Vigilance
 * When this creature dies, create two 2/2 green and white Elf Knight creature tokens with vigilance.
 */
val ConclaveCavalier = card("Conclave Cavalier") {
    manaCost = "{G}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Centaur Knight"
    oracleText = "Vigilance\n" +
        "When this creature dies, create two 2/2 green and white Elf Knight creature tokens with vigilance."
    power = 4
    toughness = 4

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 2,
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Elf", "Knight"),
            keywords = setOf(Keyword.VIGILANCE)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Just as leaves fall and the tree blooms again, one day I will fall and the Conclave will endure.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b658a309-d14c-4e20-8c76-e7b3c5156bf2.jpg?1783934140"
    }
}
