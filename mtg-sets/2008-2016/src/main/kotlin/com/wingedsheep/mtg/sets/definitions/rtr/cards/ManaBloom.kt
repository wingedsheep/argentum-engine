package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Bloom
 * {X}{G}
 * Enchantment
 *
 * This enchantment enters with X charge counters on it.
 * Remove a charge counter from this enchantment: Add one mana of any color. Activate only once each turn.
 * At the beginning of your upkeep, if this enchantment has no charge counters on it, return it to
 * its owner's hand.
 *
 * The upkeep trigger is an intervening "if": it only triggers (and only resolves) while the
 * enchantment has no charge counters on it.
 */
val ManaBloom = card("Mana Bloom") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with X charge counters on it.\n" +
        "Remove a charge counter from this enchantment: Add one mana of any color. Activate only once each turn.\n" +
        "At the beginning of your upkeep, if this enchantment has no charge counters on it, return it to its owner's hand."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.CHARGE,
            count = DynamicAmounts.castX()
        )
    )

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CHARGE, 1)
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddAnyColorMana()
        description = "Remove a charge counter from this enchantment: Add one mana of any color. " +
            "Activate only once each turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterType.CHARGE),
            ComparisonOperator.EQ,
            0
        )
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7592d88-64e8-4a31-b00a-f65d4b1867fc.jpg?1783940347"
        ruling("2012-10-01", "Mana Bloom's last ability will trigger only if it has no charge counters on it at the beginning of your upkeep. If it has one charge counter on it as your turn begins, you won't have priority to remove the counter until after this check.")
    }
}
