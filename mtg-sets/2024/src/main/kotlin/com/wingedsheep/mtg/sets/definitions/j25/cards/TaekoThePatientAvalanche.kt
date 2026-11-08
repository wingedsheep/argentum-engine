package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taeko, the Patient Avalanche — Foundations Jumpstart #39 (canonical printing)
 * {3}{U} · Legendary Creature — Turtle Ninja · 4/5
 *
 * Taeko enters tapped.
 * Whenever another creature you control leaves the battlefield, if it didn't die, scry 1 and put a
 * +1/+1 counter on Taeko.
 * Whenever Taeko attacks, you may pay {U/B}. When you do, target attacking creature can't be
 * blocked this turn.
 *
 * - "Leaves the battlefield, if it didn't die" is a per-creature (not batched) leaves trigger to any
 *   zone but a graveyard — Three Tree Scribe's `leaves(excludeTo = Zone.GRAVEYARD)` on Ninth Bridge
 *   Patrol's "another creature you control" subject. Dying is exactly "put into a graveyard from the
 *   battlefield", and the destination is fixed once the event happens, so the intervening-if reads
 *   the same at trigger time and at resolution. If Taeko leaves alongside the others, each still
 *   triggers (leaves-the-battlefield look-back); the scry happens and the counter has nowhere to go.
 * - "You may pay {U/B}. When you do, …" is a reflexive trigger (CR 603.12, Hurska Sweet-Tooth's
 *   shape): the hybrid mana is paid as the attack trigger resolves, and only then does the
 *   can't-be-blocked ability go on the stack and choose its attacking target.
 */
val TaekoThePatientAvalanche = card("Taeko, the Patient Avalanche") {
    manaCost = "{3}{U}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Turtle Ninja"
    power = 4
    toughness = 5
    oracleText = "Taeko enters tapped.\n" +
        "Whenever another creature you control leaves the battlefield, if it didn't die, scry 1 and put a " +
        "+1/+1 counter on Taeko.\n" +
        "Whenever Taeko attacks, you may pay {U/B}. When you do, target attacking creature can't be " +
        "blocked this turn."

    // Taeko enters tapped.
    replacementEffect(EntersTapped())

    // Whenever another creature you control leaves the battlefield, if it didn't die, scry 1 and put
    // a +1/+1 counter on Taeko.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).leaves(excludeTo = Zone.GRAVEYARD)
        effect = Effects.Scry(1) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Whenever Taeko attacks, you may pay {U/B}. When you do, target attacking creature can't be
    // blocked this turn.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(action = Effects.PayMana("{U/B}")) {
            val attacker = target(TargetFilter.AttackingCreature)
            effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, attacker)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "GOSSAN"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b815a1-98d8-47b9-8d6b-b30e0a5f087c.jpg?1783908859"
        ruling(
            "2024-11-08",
            "If Taeko leaves the battlefield or dies at the same time as one or more other creatures you " +
                "control leave the battlefield without dying, its second ability triggers once for each of " +
                "those other creatures. You will scry for each of those other creatures, but Taeko will not " +
                "be on the battlefield to receive +1/+1 counters.",
        )
        ruling(
            "2024-11-08",
            "You don't choose a target for Taeko's last ability at the time it triggers. Rather, a second " +
                "\"reflexive\" ability triggers when you pay {U}{B} this way. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal.",
        )
    }
}
