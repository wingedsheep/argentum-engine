package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Explosive Vegetation
 * {3}{G}
 * Sorcery
 * Search your library for up to two basic land cards, put them onto the battlefield tapped,
 * then shuffle.
 */
val ExplosiveVegetation = card("Explosive Vegetation") {
    manaCost = "{3}{G}"
    typeLine = "Sorcery"

    spell {
        effect = SearchLibraryEffect(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "John Avon"
        flavorText = "Torching Krosa would be pointless. It grows faster than it burns."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da6efd31-ab5e-46ff-80d2-9382438e302c.jpg?1562947030"
    }
}
