package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.LimitLibrarySearch
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aven Mindcensor
 * {2}{W}
 * Creature — Bird Wizard
 * 2/1
 *
 * Flash
 * Flying
 * If an opponent would search a library, that player searches the top four cards of that
 * library instead.
 *
 * The search still counts as searching the library (CR 701.23f): the whole library is shuffled
 * afterwards and "whenever a player searches" triggers fire.
 */
val AvenMindcensor = card("Aven Mindcensor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Flying\n" +
        "If an opponent would search a library, that player searches the top four cards of that library instead."

    keywords(Keyword.FLASH, Keyword.FLYING)

    replacementEffect(
        LimitLibrarySearch(
            appliesTo = EventPattern.SearchLibraryEvent(Player.EachOpponent),
            topCount = 4,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b8ede8a-5317-4662-b964-a9bd202a4aab.jpg?1783943127"
    }
}
