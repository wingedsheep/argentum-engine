package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nyx-Fleece Ram
 * {1}{W}
 * Enchantment Creature — Sheep
 * 0/5
 *
 * At the beginning of your upkeep, you gain 1 life.
 */
val NyxFleeceRam = card("Nyx-Fleece Ram") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Sheep"
    power = 0
    toughness = 5
    oracleText = "At the beginning of your upkeep, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Terese Nielsen"
        flavorText = "The gods are fascinated by innocence, perhaps because it's a quality they themselves lack."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98d13a98-e715-49a8-a59e-a2adfcdec70b.jpg?1783939456"
    }
}
