package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.ReturnFromGraveyardEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object RaiseDead {
    val definition = CardDefinition.sorcery(
        name = "Raise Dead",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Return target creature card from your graveyard to your hand."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "107",
            rarity = Rarity.COMMON,
            artist = "Charles Gillespie",
            flavorText = "The earth cannot hold that which magic commands.",
            imageUri = "https://cards.scryfall.io/normal/front/e/0/e0584553-a25e-4030-ab39-53550cba3f0b.jpg",
            scryfallId = "e0584553-a25e-4030-ab39-53550cba3f0b",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Raise Dead") {
        spell(ReturnFromGraveyardEffect(CardFilter.CreatureCard, SearchDestination.HAND))
    }
}
