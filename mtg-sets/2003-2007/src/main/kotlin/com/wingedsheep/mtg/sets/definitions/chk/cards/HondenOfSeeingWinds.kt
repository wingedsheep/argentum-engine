package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honden of Seeing Winds
 * {4}{U}
 * Legendary Enchantment — Shrine
 * At the beginning of your upkeep, draw a card for each Shrine you control.
 */
val HondenOfSeeingWinds = card("Honden of Seeing Winds") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, draw a card for each Shrine you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Martina Pilcerova"
        flavorText = "To the sorrow of all, its winds found sin in the hearts of those who once learned from its wisdom."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad732186-eeb9-4edb-a17a-51f8bac71802.jpg?1783944325"
    }
}
