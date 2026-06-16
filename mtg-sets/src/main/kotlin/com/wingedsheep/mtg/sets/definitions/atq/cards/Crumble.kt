package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crumble
 * {G}
 * Instant
 * Destroy target artifact. It can't be regenerated. That artifact's controller gains
 * life equal to its mana value.
 *
 * The destroyed artifact's controller ([EffectTarget.TargetController]) gains life equal
 * to the targeted artifact's mana value ([DynamicAmounts.targetManaValue]). The life gain
 * is sequenced before the destroy so the artifact's mana value and controller are read
 * while it is still on the battlefield; the resulting game state is identical to the
 * printed "destroy, then gain" order.
 */
val Crumble = card("Crumble") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact. It can't be regenerated. " +
        "That artifact's controller gains life equal to its mana value."

    spell {
        val artifact = target(
            "target artifact",
            TargetObject(filter = TargetFilter.Artifact)
        )
        effect = Effects.GainLife(DynamicAmounts.targetManaValue(), EffectTarget.TargetController)
            .then(Effects.Destroy(artifact, noRegenerate = true))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Jesper Myrfors"
        flavorText = "The spirits of Argoth grant new life to those who repent the folly of enslaving their labors to devices."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2101f86-8d3c-4ba8-ac42-bd3df0644280.jpg?1562939468"
    }
}
