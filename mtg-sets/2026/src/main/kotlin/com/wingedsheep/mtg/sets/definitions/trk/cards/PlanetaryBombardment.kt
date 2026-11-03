package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Planetary Bombardment — Star Trek #162
 * {R} · Sorcery
 *
 * Planetary Bombardment deals 2 damage to target creature. Planetary Bombardment deals 4 damage
 * to that creature instead if a creature died this turn.
 *
 * One damage event whose amount is a [DynamicAmount.Conditional] over
 * [Conditions.CreatureDiedThisTurn] (any player's creature), checked as the spell resolves.
 */
val PlanetaryBombardment = card("Planetary Bombardment") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Planetary Bombardment deals 2 damage to target creature. Planetary Bombardment " +
        "deals 4 damage to that creature instead if a creature died this turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            DynamicAmounts.conditional(
                condition = Conditions.CreatureDiedThisTurn,
                ifTrue = 4,
                ifFalse = 2
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Smirtouille"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d9c39ce-f598-48d1-afa0-22f9513f1483.jpg?1793013596"
    }
}
