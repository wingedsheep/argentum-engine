package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.ReturnFromGraveyardEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Graveshifter
 *
 * {3}{B} Creature — Shapeshifter 2/2
 * Changeling
 * When this creature enters, you may return target creature card from your graveyard to your hand.
 */
object Graveshifter {
    val definition = CardDefinition.creature(
        name = "Graveshifter",
        manaCost = ManaCost.parse("{3}{B}"),
        subtypes = setOf(Subtype.SHAPESHIFTER),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.CHANGELING),
        oracleText = "Changeling\nWhen this creature enters, you may return target creature card " +
                "from your graveyard to your hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "104",
            rarity = Rarity.UNCOMMON,
            artist = "Deborah Garcia",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa4a4a4a-4a4a-4a4a-4a4a-4a4a4a4a4a4a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Graveshifter") {
        keywords(Keyword.CHANGELING)

        // ETB: Return creature card from graveyard to hand
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ReturnFromGraveyardEffect(
                filter = CardFilter.CreatureCard,
                destination = SearchDestination.HAND
            ),
            optional = true
        )
    }
}
