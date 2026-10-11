package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Geth's Grimoire
 * {4}
 * Artifact — Book
 *
 * Whenever an opponent discards a card, you may draw a card.
 *
 * `Triggers.anOpponent.discards()` fires once per card discarded, so an opponent discarding two
 * cards at once gives two separate "may draw" triggers. The printed "you may" is `optional = true`.
 */
val GethsGrimoire = card("Geth's Grimoire") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "Whenever an opponent discards a card, you may draw a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        optional = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Heather Hudson"
        flavorText = "The book shrieks horribly until its cover is opened, granting the spirit inside a moment's rest from the painful stirrings of the book's contents."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a21d76d-d86c-4348-be45-f65167d2b5a9.jpg?1783944424"
    }
}
