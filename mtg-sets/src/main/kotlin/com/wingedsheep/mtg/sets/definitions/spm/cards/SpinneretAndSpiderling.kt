package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spinneret and Spiderling
 * {R}
 * Legendary Creature — Spider Human Hero
 * 1/2
 *
 * Whenever you attack with two or more Spiders, put a +1/+1 counter on Spinneret and Spiderling.
 * Whenever Spinneret and Spiderling deals 4 or more damage, exile the top card of your library.
 *   Until the end of your next turn, you may play that card.
 *
 * Modelling notes:
 * - Attack trigger: [YouAttackEvent] with `minAttackers = 2` and an `attackerFilter` of
 *   Spiders ([GameObjectFilter.Creature.withSubtype] over [Subtype.SPIDER]), bound
 *   [TriggerBinding.ANY] — it fires once per declare-attackers when at least two of the
 *   attacking creatures are Spiders, regardless of whether Spinneret and Spiderling (itself a
 *   Spider) is among them. Same shape as Landroval, Horizon Witness / Horn of the Mark.
 * - Damage trigger: [Triggers.DealsDamage] is the SELF "whenever this deals damage" event; the
 *   "4 or more" gate is a `triggerCondition` (CR 603.4 intervening-if) comparing the damage dealt
 *   in that event ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]) against 4 with
 *   [ComparisonOperator.GTE]. This mirrors the shipped Taii Wakeen / Quilled Greatwurm idiom of
 *   reading the per-event damage amount off the trigger payload; the engine models combat damage as
 *   one [com.wingedsheep.engine.core.DamageDealtEvent] per recipient, so the threshold is measured
 *   per damage event.
 * - Payoff: the "exile the top card, may play it until end of your next turn" impulse is
 *   [Patterns.Exile.impulse] with [MayPlayExpiry.UntilEndOfNextTurn] — exile one card, grant
 *   permission to play it (paying its costs) that never lapses on the current turn.
 */
val SpinneretAndSpiderling = card("Spinneret and Spiderling") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 1
    toughness = 2
    oracleText = "Whenever you attack with two or more Spiders, put a +1/+1 counter on Spinneret and Spiderling.\n" +
        "Whenever Spinneret and Spiderling deals 4 or more damage, exile the top card of your library. " +
        "Until the end of your next turn, you may play that card."

    triggeredAbility {
        trigger = TriggerSpec(
            event = YouAttackEvent(
                minAttackers = 2,
                attackerFilter = GameObjectFilter.Creature.withSubtype(Subtype.SPIDER),
            ),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.DealsDamage
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(4),
        )
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Le Vuong"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a27834b7-e763-48ac-845e-ed49f2fa6c6d.jpg?1783905332"
    }
}
