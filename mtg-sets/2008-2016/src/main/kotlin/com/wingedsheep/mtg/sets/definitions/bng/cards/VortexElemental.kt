package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vortex Elemental
 * {U}
 * Creature — Elemental
 * 0/1
 * {U}: Put this creature and each creature blocking or blocked by it on top of their owners'
 * libraries, then those players shuffle.
 * {3}{U}{U}: Target creature blocks this creature this turn if able.
 *
 * The combat pairing is gathered first — live while the Elemental is on the battlefield, from its
 * last-known snapshot if it left before the ability resolved (the card's ruling) — then the
 * Elemental (only if still on the battlefield) and the pairing are shuffled into their owners'
 * libraries. "Put on top, then shuffle" is a shuffle into the library; each affected library is
 * shuffled once.
 */
val VortexElemental = card("Vortex Elemental") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 1
    oracleText = "{U}: Put this creature and each creature blocking or blocked by it on top of their " +
        "owners' libraries, then those players shuffle.\n" +
        "{3}{U}{U}: Target creature blocks this creature this turn if able."

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.Pipeline {
            val paired = gather(CardSource.LastKnownCombatPairedWithSource)
            run(Effects.ShuffleIntoLibrary(EffectTarget.Self, fromZone = Zone.BATTLEFIELD))
            move(paired, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled))
        }
        description = "{U}: Put this creature and each creature blocking or blocked by it on top of " +
            "their owners' libraries, then those players shuffle."
    }

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Mana("{3}{U}{U}")
        effect = Effects.ForceBlock(creature)
        description = "{3}{U}{U}: Target creature blocks this creature this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Jack Wang"
        flavorText = "The sea is always hungry."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb7ca2b-c5de-4c8a-91cb-8dd8ab5387d8.jpg?1783939563"
    }
}
