package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burn the Accursed
 * {4}{R}
 * Instant
 * Burn the Accursed deals 5 damage to target creature and 2 damage to that creature's controller.
 * If that creature would die this turn, exile it instead.
 *
 * Suplex's damage-then-exile-on-death shape, with Wisecrack's [EffectTarget.TargetController]
 * for the 2 damage to the targeted creature's controller (read at resolution, before state-based
 * actions remove the creature).
 */
val BurnTheAccursed = card("Burn the Accursed") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Burn the Accursed deals 5 damage to target creature and 2 damage to that creature's controller. " +
        "If that creature would die this turn, exile it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then
            Effects.DealDamage(2, EffectTarget.TargetController) then
            Effects.MarkExileOnDeath(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "David Auden Nash"
        flavorText = "Nothing remained but a few drifting embers and the smell of smoldering hair."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff4d4e6b-564d-46da-8e32-09ed08c8ddc5.jpg?1783925604"
    }
}
