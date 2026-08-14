package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Solitary Sanctuary
 * {2}{W}
 * Enchantment
 *
 * When this enchantment enters, tap target creature an opponent controls and put a stun counter
 * on it.
 * Whenever you tap an untapped creature an opponent controls, put a +1/+1 counter on target
 * creature you control.
 *
 * The payoff is [Triggers.YouTap] — tap *attribution*, not a plain "becomes tapped" observer: only
 * a tap this enchantment's controller caused fires it, so an opponent tapping their own creature
 * (attacking, crewing, paying a cost) does nothing. "Untapped" is intrinsic to the trigger: tapping
 * is a transition (CR 603.2f), so an already-tapped creature emits no tap event.
 *
 * The entry trigger's own tap is one such tap, so this enchantment triggers its own payoff on the
 * way in — the two abilities are independent, and the +1/+1 payoff targets separately as it goes on
 * the stack.
 */
val SolitarySanctuary = card("Solitary Sanctuary") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, tap target creature an opponent controls and put a " +
        "stun counter on it. (If a permanent with a stun counter would become untapped, remove one " +
        "from it instead.)\n" +
        "Whenever you tap an untapped creature an opponent controls, put a +1/+1 counter on target " +
        "creature you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.Creature.opponentControls())
        )
        effect = Effects.Tap(victim) then Effects.AddCounters(Counters.STUN, 1, victim)
        description = "When this enchantment enters, tap target creature an opponent controls and " +
            "put a stun counter on it."
    }

    triggeredAbility {
        trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls())
        val ally = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, ally)
        description = "Whenever you tap an untapped creature an opponent controls, put a +1/+1 " +
            "counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Kasia 'Kafis' Zielińska"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d155693c-def0-4290-b662-ab9932e07fe5.jpg?1783915126"
        ruling(
            "2023-09-01",
            "You may target a creature that is already tapped with Solitary Sanctuary's first " +
                "ability. If the target creature is already tapped as it resolves, you will still " +
                "put a stun counter on it."
        )
        ruling(
            "2023-09-01",
            "Solitary Sanctuary's last ability will trigger only when an effect instructs you to tap " +
                "an opponent's creature. It won't trigger if a spell or ability you control instructs " +
                "an opponent to tap a creature they control. For example, if you control Tangle Wire " +
                "and an opponent taps an untapped creature they control as part of the resolution of " +
                "Tangle Wire's triggered ability, Solitary Sanctuary's ability won't trigger."
        )
    }
}
