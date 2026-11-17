package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Map the Wastes
 * {2}{G}
 * Sorcery
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * Bolster 1. (Choose a creature with the least toughness among creatures you control and put a
 * +1/+1 counter on it.)
 *
 * The bolstered creature is chosen as the spell resolves, after the search; bolster doesn't
 * target. With no creatures, the land search still happens and the bolster does nothing.
 */
val MapTheWastes = card("Map the Wastes") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle. " +
        "Bolster 1. (Choose a creature with the least toughness among creatures you control and put a +1/+1 counter on it.)"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true,
        ) then Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/039b61b1-79d5-44b6-8c0f-b8e8e508e5a9.jpg?1783938680"
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
