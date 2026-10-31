package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Outrage
 * {2}{R}{R}
 * Instant
 * Chandra's Outrage deals 4 damage to target creature and 2 damage to that creature's controller.
 *
 * Canonical printing: Archenemy, the card's earliest real printing. Later printings (M20, …) are
 * `Printing` rows.
 */
val ChandrasOutrage = card("Chandra's Outrage") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chandra's Outrage deals 4 damage to target creature and 2 damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature) then
            Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Christopher Moeller"
        flavorText = "\"Her mind is an incredible mix of emotion and power. Even if I could grasp it, I couldn't " +
            "hold it for long.\"\n—Jace Beleren, on Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3282db18-8564-418e-8c26-62e610b160f2.jpg?1783941910"
        ruling(
            "2019-07-12",
            "If the target creature is an illegal target by the time Chandra's Outrage tries to resolve, " +
                "the spell doesn't resolve. No player is dealt damage.",
        )
    }
}
