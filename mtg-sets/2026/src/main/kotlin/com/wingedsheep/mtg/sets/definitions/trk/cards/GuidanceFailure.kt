package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Guidance Failure
 * {3}{U}
 * Instant
 *
 * This spell costs {1} less to cast if it targets an attacking creature.
 * Target creature or Spacecraft's owner puts it on their choice of the top or bottom of their library.
 */
val GuidanceFailure = card("Guidance Failure") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if it targets an attacking creature.\n" +
        "Target creature or Spacecraft's owner puts it on their choice of the top or bottom of their library."

    spell {
        val target = target(TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype("Spacecraft")))
        effect = Effects.PutOnTopOrBottomOfLibrary(target)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 1,
                    filter = GameObjectFilter.Creature.attacking(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "David Álvarez"
        flavorText = "The ionized gasses of the Mutara Nebula wreaked havoc with sensors and shielding."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86b790e6-3c99-4a95-8327-eb90ed9380f2.jpg?1785981409"
    }
}
