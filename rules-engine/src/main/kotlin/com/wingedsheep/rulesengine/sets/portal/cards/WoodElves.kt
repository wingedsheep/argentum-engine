package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object WoodElves {
    val definition = CardDefinition.creature(
        name = "Wood Elves",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.SCOUT),
        power = 1,
        toughness = 1,
        oracleText = "When Wood Elves enters the battlefield, search your library for a Forest card and put that card onto the battlefield. Then shuffle your library.",
        metadata = ScryfallMetadata(
            collectorNumber = "195",
            rarity = Rarity.RARE,
            artist = "Rebecca Guay",
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f1fb90-5c85-46a5-802d-248cc0250921.jpg",
            scryfallId = "b7f1fb90-5c85-46a5-802d-248cc0250921",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Wood Elves") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SearchLibraryEffect(
                filter = CardFilter.HasSubtype("Forest"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = false,
                shuffleAfter = true
            )
        )
    }
}
