package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Borough Backup
 * {4}{W}
 * Sorcery
 *
 * Create two 3/2 white Hero creature tokens with vigilance.
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 *
 * Both halves are existing vocabulary: the tokens are a plain [Effects.CreateToken] batch
 * (`count = 2`), and basic landcycling is the [KeywordAbility.basicLandcycling] variant of
 * cycling — the same wiring A.I.M. Scientists and Kree Sentinel use in this set.
 */
val BoroughBackup = card("Borough Backup") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 3/2 white Hero creature tokens with vigilance.\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf(Subtype.HERO.value),
            keywords = setOf(Keyword.VIGILANCE),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/4/e4a64831-eec5-4fc9-8904-19523af3ca42.jpg?1783902804",
        )
    }

    keywordAbility(KeywordAbility.basicLandcycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Gal Or"
        flavorText = "\"Ninjas? Again? Lose our number, man.\"\n—Luke Cage, Power Man"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb753ac8-67e1-44b0-b404-37f04c2b7438.jpg?1783902978"
    }
}
