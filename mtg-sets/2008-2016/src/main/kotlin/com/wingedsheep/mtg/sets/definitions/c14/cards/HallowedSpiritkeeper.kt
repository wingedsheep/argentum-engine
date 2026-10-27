package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hallowed Spiritkeeper — Commander 2014 #8
 * {1}{W}{W} · Creature — Avatar · 3/2
 * Vigilance
 * When this creature dies, create X 1/1 white Spirit creature tokens with flying, where X is the
 * number of creature cards in your graveyard.
 *
 * X is counted as the trigger resolves, so it includes the Spiritkeeper itself if it's still in
 * your graveyard then (ruling).
 */
val HallowedSpiritkeeper = card("Hallowed Spiritkeeper") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar"
    power = 3
    toughness = 2
    oracleText = "Vigilance\n" +
        "When this creature dies, create X 1/1 white Spirit creature tokens with flying, where X is the " +
        "number of creature cards in your graveyard."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.creatureCardsInYourGraveyard(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/6/6/66c51c7f-b2d2-4a8a-84c6-57382e06fe7b.jpg?1783938799",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94ebd5bd-99b3-4371-9ca2-e0504db1458e.jpg?1783938873"
        ruling(
            "2014-11-07",
            "Count the number of creature cards in your graveyard as the triggered ability resolves, including " +
                "Hallowed Spiritkeeper itself if it's still there, to determine how many Spirits are created."
        )
    }
}
