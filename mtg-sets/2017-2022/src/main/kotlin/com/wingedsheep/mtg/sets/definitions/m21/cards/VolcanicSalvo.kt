package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volcanic Salvo (Core Set 2021 #172)
 * {10}{R}{R} · Sorcery
 *
 * This spell costs {X} less to cast, where X is the total power of creatures you control.
 * Volcanic Salvo deals 6 damage to each of up to two target creatures and/or planeswalkers.
 *
 * Ghalta's self-cost reduction (generic only, floored at the coloured pips) plus Cast into the
 * Fire's "each of up to two targets": one target requirement taking 0–2 distinct objects, with the
 * damage dealt once per chosen target.
 */
val VolcanicSalvo = card("Volcanic Salvo") {
    manaCost = "{10}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {X} less to cast, where X is the total power of creatures you control.\n" +
        "Volcanic Salvo deals 6 damage to each of up to two target creatures and/or planeswalkers."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(CostReductionSource.TotalPowerYouControl),
        )
    }

    spell {
        targets(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker), count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(6, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Torstein Nordstrand"
        flavorText = "Seeing it coming a mile away is little comfort if you can't get out of range in time."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a31b6817-9b63-4d6f-a945-459b44ad184c.jpg?1783930679"
        ruling("2020-06-23", "The cost reduction applies only to generic mana in the cost of Volcanic Salvo. It can't reduce the {R}{R} requirement.")
        ruling("2020-06-23", "If a creature's power is somehow less than 0, it subtracts from the total power of your other creatures. If the total power of your creatures is 0 or less, the cost remains {10}{R}{R}.")
        ruling("2020-06-23", "You can't target the same creature or planeswalker twice with Volcanic Salvo to deal 12 damage to it.")
        ruling("2020-06-23", "Once you begin to cast Volcanic Salvo, players can't take other actions until you've finished casting it. Notably, they can't try to change the total power of your creatures.")
        ruling("2020-06-23", "The total cost to cast Volcanic Salvo is locked in before you pay that cost. For example, if you control three 2/2 creatures, including one you can sacrifice to add {C}, the total cost is {4}{R}{R}. Then you can sacrifice the creature when you activate mana abilities just before paying the cost.")
        ruling("2020-06-23", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Volcanic Salvo). The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
    }
}
