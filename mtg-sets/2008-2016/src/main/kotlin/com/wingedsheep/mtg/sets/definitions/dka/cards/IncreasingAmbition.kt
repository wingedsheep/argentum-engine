package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Increasing Ambition — Dark Ascension #69 (canonical printing)
 * {4}{B} · Sorcery
 *
 * Search your library for a card and put that card into your hand. If this spell was cast from a
 * graveyard, instead search your library for two cards and put those cards into your hand. Then shuffle.
 * Flashback {7}{B}
 */
val IncreasingAmbition = card("Increasing Ambition") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card and put that card into your hand. If this spell was cast " +
        "from a graveyard, instead search your library for two cards and put those cards into your hand. " +
        "Then shuffle.\n" +
        "Flashback {7}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.If(
            condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
            then = Patterns.Library.searchLibrary(count = 2, destination = SearchDestination.HAND),
            otherwise = Patterns.Library.searchLibrary(count = 1, destination = SearchDestination.HAND),
        )
    }

    keywordAbility(KeywordAbility.flashback("{7}{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8f508dc-7c7d-47e8-a4ef-0e8fd99cbd74.jpg?1783940827"
    }
}
