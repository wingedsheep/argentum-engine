package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination


/**
 * Strixhaven Skycoach
 * {3}
 * Artifact — Vehicle
 * 3/2
 * Flying
 * When this Vehicle enters, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)
 */
val StrixhavenSkycoach = card("Strixhaven Skycoach") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    oracleText = "Flying\nWhen this Vehicle enters, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle.\nCrew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 2
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }
    keywordAbility(KeywordAbility.crew(2))
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87741fbb-b426-4f83-a358-587b0907f081.jpg"
    }
}
