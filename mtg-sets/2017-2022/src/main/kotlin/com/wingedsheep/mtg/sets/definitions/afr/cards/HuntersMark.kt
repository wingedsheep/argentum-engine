package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Hunter's Mark — Adventures in the Forgotten Realms #188 (canonical printing)
 * {3}{G} · Instant
 *
 * This spell costs {3} less to cast if it targets a blue permanent you don't control.
 * This spell can't be countered.
 * Target creature you control gets +1/+1 until end of turn. Then it deals damage equal to its
 * power to target creature or planeswalker you don't control.
 *
 * The discount is the Torch Breath shape: [SpellCostTarget.SelfCast] over
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on a blue permanent you don't control,
 * so only the generic {3} comes off (the {G} stands) and mana value stays 4. The spell is the
 * Rabid Gnaw bite: the pump resolves first, so the damage reads the boosted power.
 */
val HuntersMark = card("Hunter's Mark") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if it targets a blue permanent you don't control.\n" +
        "This spell can't be countered.\n" +
        "Target creature you control gets +1/+1 until end of turn. Then it deals damage equal to its " +
        "power to target creature or planeswalker you don't control."

    cantBeCountered = true

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))))
        effect = Effects.ModifyStats(1, 1, myCreature) then
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = victim,
                damageSource = myCreature,
            )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Permanent.withColor(Color.BLUE)
                        .withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou)),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62023c58-8b94-4a81-a259-a047e1edd342.jpg?1783926462"
    }
}
