package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Abzan Advantage
 * {1}{W}
 * Instant
 * Target player sacrifices an enchantment of their choice. Bolster 1. (Choose a creature with
 * the least toughness among creatures you control and put a +1/+1 counter on it.)
 *
 * The sacrifice resolves before the bolster, so the enchantment is already gone when the
 * least-toughness creature is chosen. Any player may be targeted, even one with no enchantment.
 */
val AbzanAdvantage = card("Abzan Advantage") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target player sacrifices an enchantment of their choice. Bolster 1. " +
        "(Choose a creature with the least toughness among creatures you control and put a +1/+1 counter on it.)"

    spell {
        val player = target(Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.Enchantment, 1, player) then
            Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Winona Nelson"
        flavorText = "\"To survive, you must seize every opportunity.\"\n—Daghatar the Adamant"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3ffd46f5-afd1-4130-b019-d8822e61fdfa.jpg?1783938716"
        ruling("2014-11-24", "You can target any player with Abzan Advantage, even if that player doesn't control an enchantment.")
        ruling("2014-11-24", "The enchantment will have already left the battlefield when you bolster 1.")
    }
}
