package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Searing Light
 * {W}
 * Instant
 * Destroy target attacking or blocking creature with power 2 or less.
 */
val SearingLight = card("Searing Light") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target attacking or blocking creature with power 2 or less."

    spell {
        val creature = target(TargetFilter.AttackingOrBlockingCreature.powerAtMost(2))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Marco Nelor"
        flavorText = "\"Light can lay bare our fears. It can also make them go away.\"\n—Nazra of the Stone Havens"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76dadfd8-8492-4c55-827c-cd4e6a40ae97.jpg"
    }
}
