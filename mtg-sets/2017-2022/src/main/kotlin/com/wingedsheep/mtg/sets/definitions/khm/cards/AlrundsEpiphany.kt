package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Alrund's Epiphany
 * {5}{U}{U}
 * Sorcery
 * Create two 1/1 blue Bird creature tokens with flying. Take an extra turn after this one.
 * Exile Alrund's Epiphany.
 * Foretell {4}{U}{U} (During your turn, you may pay {2} and exile this card from your hand face down.
 * Cast it on a later turn for its foretell cost.)
 *
 * The self-exile is part of resolution ([selfExile]); a countered Epiphany goes to the graveyard.
 */
val AlrundsEpiphany = card("Alrund's Epiphany") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 blue Bird creature tokens with flying. Take an extra turn after this one. " +
        "Exile Alrund's Epiphany.\nForetell {4}{U}{U} (During your turn, you may pay {2} and exile this " +
        "card from your hand face down. Cast it on a later turn for its foretell cost.)"

    spell {
        selfExile()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/1/a/1a0cb299-057d-4c39-9d73-3a3512526e4a.jpg?1783928081"
        ) then Effects.TakeExtraTurn()
    }

    keywordAbility(KeywordAbility.foretell("{4}{U}{U}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "41"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c94fcb53-a7bd-4a80-a536-9fb0eb24261a.jpg?1783928270"
        ruling(
            "2021-02-05",
            "Exiling Alrund's Epiphany as it resolves is part of its effect. If Alrund's Epiphany doesn't " +
                "resolve, it will be put into its owner's graveyard. If it's exiled this way, it's exiled " +
                "face up and doesn't become foretold."
        )
    }
}
