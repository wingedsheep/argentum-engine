package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Ride's End
 * {4}{W}
 * Instant
 *
 * This spell costs {3} less to cast if it targets a tapped permanent.
 * Exile target creature or Vehicle.
 *
 * The cost reduction is a [CostReductionSource.FixedIfAnyTargetMatches] keyed on a tapped-permanent
 * filter (the lone "creature or Vehicle" target is itself the permanent checked for tapped).
 */
val RidesEnd = card("Ride's End") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if it targets a tapped permanent.\n" +
        "Exile target creature or Vehicle."

    spell {
        val t = target(
            "creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle)),
        )
        effect = Effects.Exile(t)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Permanent.tapped(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Dmitry Burmak"
        flavorText = "Their race ended much the same way it began, walking through a sandstorm on a world that ended long ago."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f96b33b-c952-45ac-9626-40169b2bd4ef.jpg?1782687944"
    }
}
