package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reduce to Ashes (SOI #176)
 * {4}{R}  Sorcery
 *
 * Reduce to Ashes deals 5 damage to target creature. If that creature would die this turn, exile it instead.
 */
val ReduceToAshes = card("Reduce to Ashes") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Reduce to Ashes deals 5 damage to target creature. If that creature would die this turn, exile it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then
            Effects.MarkExileOnDeath(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Jason A. Engle"
        flavorText = "Previously the training center for cathars, the Elgaud Grounds are now used by the Lunarch Inquisition to punish those considered to be heretics."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2e23215-ced7-4c44-8595-dd9df3d8227b.jpg"
    }
}
