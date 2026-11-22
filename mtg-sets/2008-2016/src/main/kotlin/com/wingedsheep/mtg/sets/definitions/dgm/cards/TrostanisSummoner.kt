package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Trostani's Summoner
 * {5}{G}{W}
 * Creature — Elf Shaman
 * 1/1
 * When this creature enters, create a 2/2 white Knight creature token with vigilance, a 3/3 green
 * Centaur creature token, and a 4/4 green Rhino creature token with trample.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val TrostanisSummoner = card("Trostani's Summoner") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Shaman"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, create a 2/2 white Knight creature token with vigilance, " +
        "a 3/3 green Centaur creature token, and a 4/4 green Rhino creature token with trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/6/7/67d3d039-248a-4eb8-be5c-12959b458fea.jpg?1783940383"
        ) then Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Centaur"),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/880d5dc1-ceec-4c5f-93c2-c88b7dbfcac2.jpg?1783940382"
        ) then Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Rhino"),
            keywords = setOf(Keyword.TRAMPLE),
            imageUri = "https://cards.scryfall.io/normal/front/1/3/1331008a-ae86-4640-b823-a73be766ac16.jpg?1783940381"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1921fa4e-2256-4ef1-b2fe-874f9fbbcdf3.jpg?1783940021"
    }
}
