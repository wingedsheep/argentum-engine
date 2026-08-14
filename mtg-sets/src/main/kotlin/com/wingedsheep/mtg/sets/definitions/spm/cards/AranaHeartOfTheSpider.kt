package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Araña, Heart of the Spider
 * {1}{R}{W}
 * Legendary Creature — Spider Human Hero
 * 3/3
 *
 * Whenever you attack, put a +1/+1 counter on target attacking creature.
 * Whenever a modified creature you control deals combat damage to a player, exile the top card of
 * your library. You may play that card this turn. (Equipment, Auras you control, and counters are
 * modifications.)
 *
 * Modelling notes:
 * - Attack trigger: [Triggers.YouAttack] ([TriggerBinding.ANY]) fires once per declare-attackers;
 *   it targets an attacking creature ([TargetFilter.AttackingCreature]) and adds a +1/+1 counter.
 * - Modified-source combat-damage trigger: same shape as SP//dr, Piloted by Peni — a live
 *   [Triggers.dealsDamage] event ([DamageType.Combat] to [RecipientFilter.AnyPlayer]) whose source
 *   filter is "creature you control" narrowed by [StatePredicate.IsModified] (CR 700.4: a permanent
 *   with a counter, an Aura you control, or Equipment attached to it). The filter evaluates against
 *   current projected state at trigger time, so it is a normal source-filtered event trigger.
 * - Payoff: the "exile the top card, you may play it this turn" impulse is [Patterns.Exile.impulse]
 *   with [MayPlayExpiry.EndOfTurn] — exile one card and grant permission to play it (paying its
 *   costs) until end of the current turn.
 */
val AranaHeartOfTheSpider = card("Araña, Heart of the Spider") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 3
    toughness = 3
    oracleText = "Whenever you attack, put a +1/+1 counter on target attacking creature.\n" +
        "Whenever a modified creature you control deals combat damage to a player, exile the top " +
        "card of your library. You may play that card this turn. (Equipment, Auras you control, and " +
        "counters are modifications.)"

    triggeredAbility {
        trigger = Triggers.YouAttack
        val attacker = target(
            "target attacking creature",
            TargetCreature(filter = TargetFilter.AttackingCreature),
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, attacker)
    }

    // "modified creature you control" — CR 700.4: a permanent with a counter, an Aura you control,
    // or an Equipment attached to it.
    val modifiedCreatureYouControl = GameObjectFilter.Creature.youControl().let {
        it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            DamageType.Combat,
            RecipientFilter.AnyPlayer,
            sourceFilter = modifiedCreatureYouControl,
            binding = TriggerBinding.ANY,
        )
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b02bfa0e-f761-45e1-b35c-f44ff7c5d0e8.jpg?1783905320"
    }
}
