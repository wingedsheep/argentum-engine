package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Add Increment (Secrets of Strixhaven).
 *
 * "Increment (Whenever you cast a spell, if the amount of mana you spent is greater than
 * this creature's power or toughness, put a +1/+1 counter on this creature.)"
 *
 * Wires the full keyword from one call:
 *  - the display-only [KeywordAbility.Increment] (prints the keyword + surfaces
 *    [com.wingedsheep.sdk.core.Keyword.INCREMENT] in the base keyword set), and
 *  - a "whenever you cast a spell" triggered ability whose intervening-if compares the
 *    mana spent on that spell ([EntityNumericProperty.ManaSpent] read off the triggering
 *    spell on the stack) against this creature's power *or* toughness (CR 603.4 — the
 *    condition is checked both when the ability would trigger and again on resolution).
 *
 * "greater than this creature's power or toughness" is true when the mana spent exceeds
 * *either* characteristic, i.e. greater than the smaller of the two — modelled here as an
 * OR over the two `>` comparisons rather than a single `min`, so each side reads the live
 * projected value independently.
 */
fun CardBuilder.increment() {
    keywordAbilityList.add(KeywordAbility.Increment)

    val manaSpent = DynamicAmount.EntityProperty(
        EntityReference.Triggering,
        EntityNumericProperty.ManaSpent
    )
    val incrementCondition = AnyCondition(
        listOf(
            Compare(manaSpent, ComparisonOperator.GT, DynamicAmounts.sourcePower()),
            Compare(manaSpent, ComparisonOperator.GT, DynamicAmounts.sourceToughness())
        )
    )

    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.YouCastSpell.event,
            binding = Triggers.YouCastSpell.binding,
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            triggerCondition = incrementCondition,
            descriptionOverride = "Increment (Whenever you cast a spell, if the amount of mana " +
                "you spent is greater than this creature's power or toughness, put a +1/+1 " +
                "counter on this creature.)"
        )
    )
}
