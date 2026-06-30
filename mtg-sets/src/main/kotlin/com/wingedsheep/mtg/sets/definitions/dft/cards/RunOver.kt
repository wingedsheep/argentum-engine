package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Run Over
 * {1}{G}
 * Instant
 *
 * This spell costs {1} less to cast if it targets a Mount or Vehicle you control.
 * Target creature you control deals damage equal to its power to target creature an opponent
 * controls.
 *
 * The fight-style damage is the Rabid Bite shape: the first target (creature you control) deals
 * damage equal to its power to the second target. The cost reduction is a
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on a Mount-or-Vehicle-you-control filter —
 * the only chosen target that can match is the first ("creature you control"), since a Mount is a
 * creature and a crewed Vehicle is a creature.
 */
val RunOver = card("Run Over") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if it targets a Mount or Vehicle you control.\n" +
        "Target creature you control deals damage equal to its power to target creature an " +
        "opponent controls."

    spell {
        val mine = target("creature you control", Targets.CreatureYouControl)
        val theirs = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
            target = theirs,
            damageSource = mine,
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 1,
                    filter = (
                        GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE) or
                            GameObjectFilter.Permanent.withSubtype(Subtype("Mount"))
                        ).youControl(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642f8fdd-6c58-49a3-904c-27f717cae980.jpg?1782687821"
    }
}
