package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * By Force — Amonkhet #123 (canonical printing)
 * {X}{R} · Sorcery
 *
 * Destroy X target artifacts.
 *
 * Target count is exactly X (`targets(…, exactly = X)`, the same shape as Builder's Bane): the
 * client requires exactly the X chosen at cast time and X = 0 is a legal cast with no targets.
 * Each still-legal target is destroyed on resolution.
 */
val ByForce = card("By Force") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy X target artifacts."

    spell {
        targets(TargetFilter.Artifact, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Magali Villeneuve"
        flavorText = "\"Dissent? Your gods can't even begin to comprehend the dissent that's about to happen here!\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66cdfdb2-b47f-446d-9850-8581bd1d806c.jpg?1783936492"
    }
}
