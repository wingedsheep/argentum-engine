package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unexpected Windfall
 * {2}{R}{R}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards and create two Treasure tokens. (They're artifacts with
 * "{T}, Sacrifice this token: Add one mana of any color.")
 *
 * Canonical printing: Adventures in the Forgotten Realms (AFR) — the earliest real
 * expansion printing.
 */
val UnexpectedWindfall = card("Unexpected Windfall") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card.\n" +
        "Draw two cards and create two Treasure tokens. (They're artifacts with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")"

    additionalCost(Costs.additional.DiscardCards())

    spell {
        effect = Effects.DrawCards(2) then Effects.CreateTreasure(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Alayna Danner"
        flavorText = "Fortune favors the fortunate."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bae6a5fb-39f5-4cf8-85f7-661cb4570507.jpg?1783926470"
    }
}
