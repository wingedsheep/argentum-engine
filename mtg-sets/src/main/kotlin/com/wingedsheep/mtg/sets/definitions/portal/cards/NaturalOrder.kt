package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Natural Order
 * {2}{G}{G}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a green creature.
 * Search your library for a green creature card, put it onto the battlefield, then shuffle.
 */
val NaturalOrder = card("Natural Order") {
    manaCost = "{2}{G}{G}"
    typeLine = "Sorcery"

    additionalCost(AdditionalCost.SacrificePermanent(
        filter = CardFilter.And(listOf(
            CardFilter.CreatureCard,
            CardFilter.HasColor(Color.GREEN)
        ))
    ))

    spell {
        effect = SearchLibraryEffect(
            filter = CardFilter.And(listOf(
                CardFilter.CreatureCard,
                CardFilter.HasColor(Color.GREEN)
            )),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Alan Rabinowitz"
        flavorText = "Nature's cycle continues: from life, life springs forth."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cecb34f8-6961-4c27-9368-26d156714d7b.jpg"
        ruling(
            "6/8/2016",
            "Sacrificing a green creature is part of Natural Order's cost. You can't sacrifice more creatures to search for more creature cards, and you can't cast Natural Order at all if you control no green creatures."
        )
        ruling(
            "6/8/2016",
            "Players can respond to this spell only after it's been cast and all its costs have been paid. No one can try to destroy the creature you sacrificed to stop you from casting this spell or to make you sacrifice a different one."
        )
    }
}
