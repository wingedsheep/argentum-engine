package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pigment Storm — Strixhaven: School of Mages #111 (canonical printing)
 * {3}{R}{R} · Sorcery
 *
 * Pigment Storm deals 5 damage to target creature. Excess damage is dealt to that creature's
 * controller instead.
 *
 * Gandalf's Sanction's shape with a fixed 5: [Effects.DealDamageExcessToController] deals the
 * creature only lethal damage (marked damage and deathtouch aware, CR 120.4a) and the rest to its
 * controller, as one self-replacement (ruling). An illegal target fizzles the spell, so no player
 * is dealt damage either.
 */
val PigmentStorm = card("Pigment Storm") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Pigment Storm deals 5 damage to target creature. Excess damage is dealt to that " +
        "creature's controller instead."

    spell {
        val victim = target(TargetFilter.Creature)
        effect = Effects.DealDamageExcessToController(amount = DynamicAmounts.fixed(5), target = victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Bayard Wu"
        flavorText = "When art projects collapse, Prismari students revel in the beauty of destruction. Everyone else runs for cover."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d285a7a1-bb7e-4a78-a49f-c2add62b829a.jpg?1783927350"
        ruling("2021-04-16", "Excess damage has been dealt to a creature if the damage dealt to it is greater than lethal damage. Usually, this means damage greater than its toughness, although damage already marked on the creature is taken into account.")
        ruling("2021-04-16", "If the target creature is an illegal target by the time Pigment Storm tries to resolve, the spell won't resolve. It won't deal damage to any player.")
        ruling("2021-04-16", "Pigment Storm's replacement effect is a self-replacement effect. It is applied before any other replacement effects that would change the amount of damage dealt to the target creature and its controller.")
    }
}
