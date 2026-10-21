package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Animist's Might
 * {2}{G}
 * Sorcery
 * This spell costs {2} less to cast if it targets a legendary creature you control.
 * Target creature you control deals damage equal to twice its power to target creature or
 * planeswalker you don't control.
 *
 * The discount is the Ephara's Dispersal shape — [CostReductionSource.FixedIfAnyTargetMatches]
 * over the spell's own chosen targets, gated on a legendary creature you control. Only the generic
 * {2} is reduced, so the mana value stays 3. The damage is the Polliwallop shape: twice the first
 * target's power on resolution, dealt by that creature.
 */
val AnimistsMight = card("Animist's Might") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if it targets a legendary creature you control.\n" +
        "Target creature you control deals damage equal to twice its power to target creature or " +
        "planeswalker you don't control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.legendary().youControl(),
                ),
            ),
        )
    }

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(myCreature) * 2,
            target = victim,
            damageSource = myCreature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "John Tedrick"
        flavorText = "The creature was unlike any Nissa had seen, but she dealt with it as she would any threat."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/8525ae50-7438-492a-98ae-53f0ffeaab51.jpg?1783916520"
        ruling(
            "2023-05-12",
            "The cost-reduction ability of Animist's Might doesn't change its mana cost or mana value, " +
                "only the total cost you pay. Specifically, the mana value of Animist's Might is always 3."
        )
    }
}
