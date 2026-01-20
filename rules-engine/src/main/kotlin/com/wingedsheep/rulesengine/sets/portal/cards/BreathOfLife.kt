package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.ReturnFromGraveyardEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object BreathOfLife {
    val definition = CardDefinition.sorcery(
        name = "Breath of Life",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "Return target creature card from your graveyard to the battlefield."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "10",
            rarity = Rarity.COMMON,
            artist = "DiTerlizzi",
            imageUri = "https://cards.scryfall.io/normal/front/b/c/bcea5e09-6385-41df-970b-ac26c9b46127.jpg",
            scryfallId = "bcea5e09-6385-41df-970b-ac26c9b46127",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Breath of Life") {
        spell(ReturnFromGraveyardEffect(CardFilter.CreatureCard, SearchDestination.BATTLEFIELD))
    }
}
