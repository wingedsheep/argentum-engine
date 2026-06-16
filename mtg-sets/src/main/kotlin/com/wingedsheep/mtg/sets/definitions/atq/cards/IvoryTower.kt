package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ivory Tower
 * {1}
 * Artifact
 * At the beginning of your upkeep, you gain X life, where X is the number of cards
 * in your hand minus 4.
 *
 * Upkeep trigger gaining life equal to (cards in hand - 4), floored at 0 via
 * [DynamicAmount.IfPositive] so a hand of 4 or fewer cards gains nothing.
 */
val IvoryTower = card("Ivory Tower") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, you gain X life, where X is the number of cards in your hand minus 4."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.GainLife(
            DynamicAmount.IfPositive(
                DynamicAmount.Subtract(
                    DynamicAmounts.zone(Player.You, Zone.HAND).count(),
                    DynamicAmount.Fixed(4)
                )
            )
        )
        description = "At the beginning of your upkeep, you gain X life, where X is the number of cards in your hand minus 4."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Margaret Organ-Kean"
        flavorText = "Valuing scholarship above all else, the inhabitants of the Ivory Tower reward those who sacrifice power for knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5f23039-45ca-4c15-af50-bfd40ea26453.jpg?1562929973"
    }
}
