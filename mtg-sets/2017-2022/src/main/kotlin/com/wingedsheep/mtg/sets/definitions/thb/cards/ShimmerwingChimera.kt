package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shimmerwing Chimera
 * {3}{U}
 * Enchantment Creature — Chimera
 * 3/2
 * Flying
 * At the beginning of your upkeep, return up to one other target enchantment you control to its
 * owner's hand.
 *
 * An upkeep trigger with an optional "other enchantment you control" target (`excludeSelf`, so the
 * Chimera — itself an enchantment — can't bounce itself) feeding [Effects.ReturnToHand].
 */
val ShimmerwingChimera = card("Shimmerwing Chimera") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Chimera"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, return up to one other target enchantment you control to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val enchantment = target(
            TargetFilter(GameObjectFilter.Enchantment.youControl(), excludeSelf = true),
            optional = true,
        )
        effect = Effects.ReturnToHand(enchantment)
        description = "At the beginning of your upkeep, return up to one other target enchantment you control " +
            "to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Christopher Burdett"
        flavorText = "It swims upon the winds and soars through the waves."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3b54bea-f2c5-4000-9cbd-04c03a1d2ea2.jpg?1783931578"
    }
}
