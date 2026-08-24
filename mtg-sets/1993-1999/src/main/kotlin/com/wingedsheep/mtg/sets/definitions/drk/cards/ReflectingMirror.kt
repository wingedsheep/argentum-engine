package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Reflecting Mirror
 * {4}
 * Artifact
 * {X}, {T}: Change the target of target spell with a single target if that target is you. The new
 * target must be a player. X is twice the mana value of that spell.
 *
 * The redirect itself is the existing `ChangeTargetEffect` with both of this card's printed
 * restrictions turned on: it only fires when the spell's single target is you, and the replacement
 * target must be a player. The new-target filter narrows the spell's *own* legal-target list rather
 * than replacing it, so the redirect can never make an otherwise-illegal choice legal.
 *
 * **Known divergence: where X is enforced.** The printed X is *computed*, not chosen — "X is twice
 * the mana value of that spell" — but activated-ability cost calculation has no access to the
 * ability's chosen targets, so the cost cannot be derived from the spell being redirected. X is
 * therefore chosen by the player at activation, and the constraint is enforced at **resolution**:
 * the ability does nothing unless the X paid was at least twice the target spell's mana value.
 *
 * The practical difference is what happens when you underpay. The printed card makes an underpaid
 * activation impossible; here it is possible but wasted — the mana and the tap are spent and the
 * spell is not redirected. What matters is that the redirect can never be had for less than its
 * printed price. Fixing this properly means threading the chosen targets into ability cost
 * calculation, which is a cost-system change rather than a card.
 */
val ReflectingMirror = card("Reflecting Mirror") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{X}, {T}: Change the target of target spell with a single target if that target " +
        "is you. The new target must be a player. X is twice the mana value of that spell."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        target = Targets.SpellOrAbilityWithSingleTarget
        effect = ConditionalEffect(
            condition = Conditions.CompareAmounts(
                DynamicAmount.XValue,
                ComparisonOperator.GTE,
                DynamicAmount.Multiply(
                    DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.ManaValue,
                    ),
                    multiplier = 2,
                ),
            ),
            effect = Effects.ChangeTarget(
                newTargetMustBePlayer = true,
                onlyIfCurrentTargetIsController = true,
            ),
        )
        description = "{X}, {T}: Change the target of target spell with a single target if that " +
            "target is you. The new target must be a player. X is twice the mana value of that spell."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d551ff93-d8da-4c21-bc3c-6451c0dde07e.jpg?1783947925"
    }
}
