package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Firecannon Blast
 * {1}{R}{R}
 * Sorcery
 * Firecannon Blast deals 3 damage to target creature.
 * Raid — Firecannon Blast deals 6 damage instead if you attacked this turn.
 */
val FirecannonBlast = card("Firecannon Blast") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Firecannon Blast deals 3 damage to target creature.\nRaid — Firecannon Blast deals 6 damage instead if you attacked this turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.YouAttackedThisTurn,
            then = Effects.DealDamage(6, t),
            otherwise = Effects.DealDamage(3, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Svetlin Velinov"
        flavorText = "Goblins' fearlessness and diminutive size make them the perfect cannoneers."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1f083c3-d7b0-4d3d-8551-af52c7883d83.jpg?1783935746"
    }
}
