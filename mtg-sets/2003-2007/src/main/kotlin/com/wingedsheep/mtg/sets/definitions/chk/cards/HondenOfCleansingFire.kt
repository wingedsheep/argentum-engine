package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honden of Cleansing Fire
 * {3}{W}
 * Legendary Enchantment — Shrine
 * At the beginning of your upkeep, you gain 2 life for each Shrine you control.
 *
 * The Shrine count is read on resolution and includes the Honden itself.
 */
val HondenOfCleansingFire = card("Honden of Cleansing Fire") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, you gain 2 life for each Shrine you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count() * 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Greg Staples"
        flavorText = "To the sorrow of all, its fire was turned toward those who worshipped it."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/463f598a-7de0-4691-a61a-fe24c29f9e1b.jpg?1783944339"
    }
}
