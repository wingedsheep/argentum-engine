package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.ReturnFromGraveyardEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object Gravedigger {
    val definition = CardDefinition.creature(
        name = "Gravedigger",
        manaCost = ManaCost.parse("{3}{B}"),
        subtypes = setOf(Subtype.ZOMBIE),
        power = 2,
        toughness = 2,
        oracleText = "When Gravedigger enters the battlefield, you may return target creature card from your graveyard to your hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "95",
            rarity = Rarity.UNCOMMON,
            artist = "Scott M. Fischer",
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b979d70e-d514-420f-886c-f60e2bb1861f.jpg",
            scryfallId = "b979d70e-d514-420f-886c-f60e2bb1861f",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Gravedigger") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ReturnFromGraveyardEffect(
                filter = CardFilter.CreatureCard,
                destination = SearchDestination.HAND
            )
        )
    }
}
