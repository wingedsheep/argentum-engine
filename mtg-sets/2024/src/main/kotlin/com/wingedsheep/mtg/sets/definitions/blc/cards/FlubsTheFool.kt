package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Flubs, the Fool — Bloomburrow Commander #356
 * {G}{U}{R} · Legendary Creature — Frog Scout · 0/5
 *
 * You may play an additional land on each of your turns.
 * Whenever you play a land or cast a spell, draw a card if you have no cards in hand.
 * Otherwise, discard a card.
 *
 * "Play a land or cast a spell" is two triggered abilities (each event fires exactly one of
 * them, as Shadow of the Goblin does). The hand check is made on resolution, so a spell cast
 * from your last card still draws, while responding with another card leaves you discarding.
 */
val FlubsTheFool = card("Flubs, the Fool") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Frog Scout"
    power = 0
    toughness = 5
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Whenever you play a land or cast a spell, draw a card if you have no cards in hand. " +
        "Otherwise, discard a card."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    triggeredAbility {
        trigger = Triggers.you.playsLand()
        effect = Effects.If(
            condition = Conditions.EmptyHand,
            then = Effects.DrawCards(1),
            otherwise = Patterns.Hand.discardCards(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.If(
            condition = Conditions.EmptyHand,
            then = Effects.DrawCards(1),
            otherwise = Patterns.Hand.discardCards(1),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "356"
        artist = "Adam Rex"
        flavorText = "He knows exactly where he's going and exactly how he isn't getting there."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41e58eb8-e5b9-4ef6-be1f-00e28cebb998.jpg?1783910621"
    }
}
