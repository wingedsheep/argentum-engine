package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * The Birth of Meletis
 * {1}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.
 * II — Create a 0/4 colorless Wall artifact creature token with defender.
 * III — You gain 2 life.
 *
 * Chapter I is The Restoration of Eiganjo's Plains tutor.
 */
private const val WALL_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/d/b/dbdd5d53-65e7-42c3-a325-791f11335e62.jpg?1783931450"

val TheBirthOfMeletis = card("The Birth of Meletis") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Search your library for a basic Plains card, reveal it, put it into your hand, then shuffle.\n" +
        "II — Create a 0/4 colorless Wall artifact creature token with defender.\n" +
        "III — You gain 2 life."

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    sagaChapter(2) {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 4,
            colors = emptySet(),
            creatureTypes = setOf("Wall"),
            keywords = setOf(Keyword.DEFENDER),
            artifactToken = true,
            imageUri = WALL_TOKEN_IMAGE,
        )
    }

    sagaChapter(3) {
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d824f8bd-58f8-4796-80b5-5cd3033d35e8.jpg?1783931602"
    }
}
