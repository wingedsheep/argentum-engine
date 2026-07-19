package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Truck Toss (MSH #157) — {2}{R}{R} Instant
 *
 * This spell costs {2} less to cast if you control a Vehicle.
 * Truck Toss deals 4 damage to any target.
 *
 * The reduction is a self-cast [ModifySpellCost] whose whole modification is gated on a state
 * condition ([CostGating.OnlyIf] over "you control a Vehicle") rather than baked into the
 * amount — the shape the SDK reference prescribes for conditional cost reduction. Only the
 * generic {2} is reduced; the {R}{R} pips are untouched. A Vehicle is any permanent with the
 * Vehicle subtype, crewed or not.
 */
val TruckToss = card("Truck Toss") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if you control a Vehicle.\n" +
        "Truck Toss deals 4 damage to any target."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.YouControl(GameObjectFilter.Any.withSubtype(Subtype.VEHICLE))
            ),
        )
    }

    spell {
        val victim = target("any target", Targets.Any)
        effect = Effects.DealDamage(4, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Alexander Skripnikov"
        flavorText = "\"NO! I LOVE TACOS!\"\n—Hulk"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60f02fbf-1416-48a3-bf94-f27509f6983a.jpg?1783902923"
    }
}
