package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ChangelingWayfinder {
    val definition = CardDefinition.creature(
        name = "Changeling Wayfinder",
        manaCost = ManaCost.parse("{3}"),
        subtypes = setOf(Subtype.of("Shapeshifter")),
        power = 1,
        toughness = 2,
        keywords = setOf(Keyword.CHANGELING),
        oracleText = "Changeling (This card is every creature type.)\n" +
                "When Changeling Wayfinder enters the battlefield, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle.",
        metadata = ScryfallMetadata(
            collectorNumber = "1",
            rarity = Rarity.COMMON,
            artist = "Quintin Gleim",
            flavorText = "\"No map. No complaints.\"",
            imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6061aa-a4da-4115-85b6-d0aa22f2386c.jpg",
            scryfallId = "6c6061aa-a4da-4115-85b6-d0aa22f2386c",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Changeling Wayfinder") {
        keywords(Keyword.CHANGELING)

        triggered(
            trigger = OnEnterBattlefield(),
            effect = SearchLibraryEffect(
                filter = CardFilter.BasicLandCard,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            ),
            // "You may search..." -> Optional trigger
            optional = true
        )
    }
}