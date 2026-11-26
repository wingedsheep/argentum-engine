package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Mwonvuli Beast Tracker
 * {1}{G}{G}
 * Creature — Human Scout
 * 2/1
 * When this creature enters, search your library for a creature card with deathtouch, hexproof,
 * reach, or trample and reveal it. Shuffle and put that card on top.
 */
val MwonvuliBeastTracker = card("Mwonvuli Beast Tracker") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, search your library for a creature card with deathtouch, " +
        "hexproof, reach, or trample and reveal it. Shuffle and put that card on top."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.withCardPredicate(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.HasKeyword(Keyword.DEATHTOUCH),
                        CardPredicate.HasKeyword(Keyword.HEXPROOF),
                        CardPredicate.HasKeyword(Keyword.REACH),
                        CardPredicate.HasKeyword(Keyword.TRAMPLE),
                    )
                )
            ),
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Zoltan Boros"
        flavorText = "\"The more dangerous, the better.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0034d32c-cc82-48d7-a913-d58cc3d3afeb.jpg?1783940470"
        ruling(
            "2012-07-01",
            "The creature card must have one of the listed abilities, not an ability that grants one " +
                "of the listed abilities to the creature. For example, you couldn't put Prized Elephant, " +
                "which has \"{G}: Prized Elephant gains trample until end of turn,\" on top of your " +
                "library this way.",
        )
    }
}
