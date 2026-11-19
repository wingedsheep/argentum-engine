package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx's Disciple
 * {3}{U}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * Flying
 * Inspired — Whenever this creature becomes untapped, draw a card.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SphinxsDisciple = card("Sphinx's Disciple") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\nInspired — Whenever this creature becomes untapped, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Ryan Alexander Lee"
        flavorText = "Few dare to brave the Dakra Isles. Those who return are forever changed."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba33f00d-dbed-4bfb-a295-90f729be98de.jpg?1783939565"
    }
}
