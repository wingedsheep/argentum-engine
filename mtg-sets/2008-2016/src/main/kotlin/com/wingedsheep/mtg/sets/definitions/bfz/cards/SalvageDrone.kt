package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Salvage Drone
 * {U}
 * Creature — Eldrazi Drone
 * 1/1
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * When this creature dies, you may draw a card. If you do, discard a card.
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`).
 */
val SalvageDrone = card("Salvage Drone") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "When this creature dies, you may draw a card. If you do, discard a card."

    keywords(Keyword.DEVOID, Keyword.INGEST)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b8ce2a9-9d8e-442b-bc12-74545ceb65bd.jpg?1783938211"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
