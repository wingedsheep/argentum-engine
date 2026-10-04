package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OpponentsMustAttackYou

/**
 * Trove of Temptation
 * {3}{R}
 * Enchantment
 * Each opponent must attack you or a planeswalker you control with at least one creature each
 * combat if able.
 * At the beginning of your end step, create a Treasure token.
 */
val TroveOfTemptation = card("Trove of Temptation") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Each opponent must attack you or a planeswalker you control with at least one creature each combat if able.\nAt the beginning of your end step, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    staticAbility {
        ability = OpponentsMustAttackYou
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb2df914-bb56-449e-9ef8-8b1012a76f64.jpg"
    }
}
