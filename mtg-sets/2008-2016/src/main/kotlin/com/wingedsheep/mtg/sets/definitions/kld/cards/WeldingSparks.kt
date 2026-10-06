package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Welding Sparks
 * {2}{R}
 * Instant
 * Welding Sparks deals X damage to target creature, where X is 3 plus the number of artifacts
 * you control.
 */
val WeldingSparks = card("Welding Sparks") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Welding Sparks deals X damage to target creature, where X is 3 plus the number of artifacts you control."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            3 + DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            t,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Raymond Swanland"
        flavorText = "\"The fires of invention burn in all of us. I fight for the freedom to unleash those flames.\"\n—Pia Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe2d98db-64c4-40b4-b6c8-61da8cc09f42.jpg?1783937186"
        ruling("2020-11-10", "If you control zero artifacts, Welding Sparks deals 3 damage to the target creature.")
        ruling("2020-11-10", "The number of artifacts you control is counted only as Welding Sparks resolves.")
    }
}
