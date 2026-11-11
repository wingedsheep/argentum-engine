package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vile Aggregate
 * {2}{R}
 * Creature — Eldrazi Drone
 * * /5
 * Devoid (This card has no color.)
 * Vile Aggregate's power is equal to the number of colorless creatures you control.
 * Trample
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 *
 * The starred power is a characteristic-defining ability (`dynamicPower`). The StateProjector
 * resolves it against the in-progress projection, so the colorless check reads projected colour:
 * devoid creatures (Vile Aggregate itself included) count, colorless artifact creatures count,
 * and a creature given a colour doesn't.
 */
val VileAggregate = card("Vile Aggregate") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "Vile Aggregate's power is equal to the number of colorless creatures you control.\n" +
        "Trample\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)"

    keywords(Keyword.DEVOID, Keyword.TRAMPLE, Keyword.INGEST)

    dynamicPower(
        DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)
        ).count()
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ec66525-a7e9-450f-83df-03d9957837d5.jpg?1783938195"
        ruling("2015-08-25", "The ability that defines Vile Aggregate's power works in all zones, not just the battlefield. If Vile Aggregate is on the battlefield, it will count itself.")
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
