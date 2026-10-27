package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Forging the Tyrite Sword
 * {1}{R}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Create a Treasure token.
 * III — Search your library for a card named Halvar, God of Battle or an Equipment card, reveal it,
 * put it into your hand, then shuffle.
 *
 * Halvar is a modal double-faced card; in the library only its front face exists, so it is found by
 * name rather than by its Equipment back face (Sword of the Realms).
 */
val ForgingTheTyriteSword = card("Forging the Tyrite Sword") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Create a Treasure token.\n" +
        "III — Search your library for a card named Halvar, God of Battle or an Equipment card, reveal it, " +
        "put it into your hand, then shuffle."

    sagaChapter(1) {
        effect = Effects.CreateTreasure(1)
    }

    sagaChapter(2) {
        effect = Effects.CreateTreasure(1)
    }

    sagaChapter(3) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.named("Halvar, God of Battle") or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ce46100-461d-424e-afa4-7a0bb7d0a822.jpg?1783928197"
    }
}
