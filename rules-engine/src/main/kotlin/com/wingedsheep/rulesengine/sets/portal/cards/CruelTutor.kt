package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object CruelTutor {
    val definition = CardDefinition.sorcery(
        name = "Cruel Tutor",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Search your library for a card, then shuffle and put that card on top. You lose 2 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "87",
            rarity = Rarity.RARE,
            artist = "Kev Walker",
            flavorText = "The more you pay for the lesson, the better you'll remember it.",
            imageUri = "https://cards.scryfall.io/normal/front/6/c/6c05bfb5-dd36-44c5-a60d-43f7f8c68a6b.jpg",
            scryfallId = "6c05bfb5-dd36-44c5-a60d-43f7f8c68a6b",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Cruel Tutor") {
        spell(
            SearchLibraryEffect(
                filter = CardFilter.AnyCard,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                shuffleAfter = true,
                reveal = false
            ) then LoseLifeEffect(2, EffectTarget.Controller)
        )
    }
}
