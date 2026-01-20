package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object NaturesLore {
    val definition = CardDefinition.sorcery(
        name = "Nature's Lore",
        manaCost = ManaCost.parse("{1}{G}"),
        oracleText = "Search your library for a Forest card and put that card onto the battlefield. Then shuffle your library."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "178",
            rarity = Rarity.COMMON,
            artist = "Terese Nielsen",
            flavorText = "Nature's secrets can be read on every tree, every branch, every leaf.",
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5242227-d033-4e03-b1e6-b334b17bb158.jpg",
            scryfallId = "d5242227-d033-4e03-b1e6-b334b17bb158",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Nature's Lore") {
        spell(
            SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = true
            )
        )
    }
}
