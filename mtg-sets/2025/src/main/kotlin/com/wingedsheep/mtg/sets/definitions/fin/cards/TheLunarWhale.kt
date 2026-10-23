package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * The Lunar Whale
 * {3}{U}
 * Legendary Artifact — Vehicle
 * 3/5
 *
 * Flying
 * You may look at the top card of your library any time.
 * As long as The Lunar Whale attacked this turn, you may play the top card of your library.
 * Crew 1
 *
 * The "play the top card" permission is non-revealing — only the controller sees the top card
 * (via [LookAtTopOfLibrary]); it is NOT publicly revealed like Future Sight's
 * [com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary]. "Play the top card" (any card type) is
 * modeled as [PlayLandsAndCastFilteredFromTopOfLibrary] with an unrestricted [GameObjectFilter.Any]
 * spell filter — play lands and cast any spell from the top — gated by a
 * [Conditions.SourceAttackedThisTurn] conditional static so the permission only applies after the
 * Whale has attacked this turn.
 */
val TheLunarWhale = card("The Lunar Whale") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Vehicle"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "You may look at the top card of your library any time.\n" +
        "As long as The Lunar Whale attacked this turn, you may play the top card of your library.\n" +
        "Crew 1"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        condition = Conditions.SourceAttackedThisTurn
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae875471-346c-4a76-b26f-b7205dad5b80.jpg?1782686550"
    }
}
