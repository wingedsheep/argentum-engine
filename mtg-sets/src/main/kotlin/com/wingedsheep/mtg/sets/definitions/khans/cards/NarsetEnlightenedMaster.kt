package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Narset, Enlightened Master
 * {3}{U}{R}{W}
 * Legendary Creature — Human Monk
 * First strike, hexproof
 * Whenever Narset, Enlightened Master attacks, exile the top four cards of your library.
 * Until end of turn, you may cast noncreature, nonland cards from among those cards
 * without paying their mana costs.
 * 3/2
 */
val NarsetEnlightenedMaster = card("Narset, Enlightened Master") {
    manaCost = "{3}{U}{R}{W}"
    typeLine = "Legendary Creature — Human Monk"
    oracleText = "First strike, hexproof\nWhenever Narset, Enlightened Master attacks, exile the top four cards of your library. Until end of turn, you may cast noncreature, nonland cards from among those cards without paying their mana costs."
    power = 3
    toughness = 2

    keywords(Keyword.FIRST_STRIKE, Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CompositeEffect(
            listOf(
                // Exile the top 4 cards of your library
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "exiledCards"
                ),
                MoveCollectionEffect(
                    from = "exiledCards",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // Filter to noncreature, nonland cards
                FilterCollectionEffect(
                    from = "exiledCards",
                    filter = CollectionFilter.MatchesFilter(
                        GameObjectFilter.Noncreature and GameObjectFilter.Nonland
                    ),
                    storeMatching = "castable"
                ),
                // Grant free casting permission until end of turn
                GrantMayPlayFromExileEffect("castable"),
                GrantPlayWithoutPayingCostEffect("castable")
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f210adb7-b389-4672-a3eb-0ced9bfe190c.jpg?1562795852"
        ruling("2014-09-20", "The cards are exiled face up. Casting the noncreature cards exiled this way follows the normal rules for casting those cards.")
        ruling("2014-09-20", "You can't play any land cards exiled with Narset.")
        ruling("2014-09-20", "If you cast a spell \"without paying its mana cost,\" you can't pay any other alternative costs, including casting it face down using the morph ability.")
        ruling("2014-09-20", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2014-09-20", "Any exiled cards you don't cast remain in exile.")
    }
}
