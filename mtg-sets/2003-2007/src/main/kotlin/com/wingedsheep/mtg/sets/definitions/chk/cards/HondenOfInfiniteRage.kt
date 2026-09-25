package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honden of Infinite Rage
 * {2}{R}
 * Legendary Enchantment — Shrine
 * At the beginning of your upkeep, Honden of Infinite Rage deals damage to any target equal to the
 * number of Shrines you control.
 *
 * The target is chosen when the trigger goes on the stack; the Shrine count is read on resolution.
 */
val HondenOfInfiniteRage = card("Honden of Infinite Rage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, Honden of Infinite Rage deals damage to any target " +
        "equal to the number of Shrines you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
            t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "John Avon"
        flavorText = "To the sorrow of all, its rage became focused on those who once stoked it."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b254f341-aac1-433b-a739-fee8bd7fdf69.jpg?1783944300"
    }
}
