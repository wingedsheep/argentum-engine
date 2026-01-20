package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.ConditionalEffect
import com.wingedsheep.rulesengine.ability.OpponentControlsMoreLands
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object GiftOfEstates {
    val definition = CardDefinition.sorcery(
        name = "Gift of Estates",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "If an opponent controls more lands than you, search your library for up to " +
                "three Plains cards, reveal them, put them into your hand, then shuffle."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "17",
            rarity = Rarity.RARE,
            artist = "Kaja Foglio",
            imageUri = "https://cards.scryfall.io/normal/front/3/4/342b5afe-544f-4fa1-a833-4e0590b41eed.jpg",
            scryfallId = "342b5afe-544f-4fa1-a833-4e0590b41eed",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Gift of Estates") {
        spell(
            ConditionalEffect(
                condition = OpponentControlsMoreLands,
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Plains"),
                    count = 3,
                    destination = SearchDestination.HAND,
                    entersTapped = false,
                    shuffleAfter = true,
                    reveal = true
                )
            )
        )
    }
}
