package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Titan's Presence — Battle for Zendikar #14 (canonical printing)
 * {3} · Instant
 *
 * As an additional cost to cast this spell, reveal a colorless creature card from your hand.
 * Exile target creature if its power is less than or equal to the revealed card's power.
 *
 * The revealed card stays in hand (revealing moves nothing); `EffectTarget.RevealedAsCost` reads
 * its power as captured when the cost was paid, which is also the ruling's "as it last existed in
 * your hand" if it has left by resolution. The target's power is checked as the spell resolves.
 */
val TitansPresence = card("Titan's Presence") {
    manaCost = "{3}"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, reveal a colorless creature card from your hand.\n" +
        "Exile target creature if its power is less than or equal to the revealed card's power."

    additionalCost(
        Costs.additional.RevealFromHand(
            filter = GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)
        )
    )

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.powerOf(creature),
                ComparisonOperator.LTE,
                DynamicAmounts.powerOf(EffectTarget.RevealedAsCost()),
            ),
            then = Effects.Exile(creature),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Slawomir Maniak"
        flavorText = "Dust and memory are all that remain in Ulamog's wake."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d5e3ab-9719-4918-af4f-25bda5401191.jpg?1783938222"
    }
}
