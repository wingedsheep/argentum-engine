package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heliod's Intervention
 * {X}{W}{W}
 * Instant
 * Choose one —
 * • Destroy X target artifacts and/or enchantments.
 * • Target player gains twice X life.
 *
 * Both modes read the one X paid at cast time (Confront the Past's shape). The first is Builder's
 * Bane's exact-X target count (`targets(…, exactly = X)`, CR 601.2c) over artifacts and
 * enchantments, each destroyed through [Effects.ForEachTarget]. The second is a single life gain of
 * `X * 2` (2020-01-24 ruling: one gain of twice X, not two gains of X).
 */
val HeliodsIntervention = card("Heliod's Intervention") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy X target artifacts and/or enchantments.\n" +
        "• Target player gains twice X life."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy X target artifacts and/or enchantments") {
                targets(TargetFilter.ArtifactOrEnchantment, exactly = DynamicAmounts.xValue())
                effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
            }
            mode("Target player gains twice X life") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(DynamicAmounts.xValue() * 2, player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Daarken"
        flavorText = "\"I bring a dawn that no night can follow.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1c16dd-38e9-4974-b570-8678c114a549.jpg?1783931596"
        ruling("2020-01-24", "The second mode of Heliod's Intervention causes the player to gain one amount of life equal to twice X. It doesn't cause the player to gain X life then gain X life again.")
    }
}
