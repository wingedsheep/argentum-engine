package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Quicksmith Genius
 * {2}{R}
 * Creature — Human Artificer
 * 3/2
 * Whenever an artifact you control enters, you may discard a card. If you do, draw a card.
 */
val QuicksmithGenius = card("Quicksmith Genius") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    oracleText = "Whenever an artifact you control enters, you may discard a card. If you do, draw a card."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Discard(1),
                then = Effects.DrawCards(1),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "David Gaillet"
        flavorText = "Quicksmiths are eager to jump from one project to the next, relying on flashes of inspiration rather than careful research."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9f1dc85-3cda-45d6-8d65-3cb93854bb16.jpg"
    }
}
