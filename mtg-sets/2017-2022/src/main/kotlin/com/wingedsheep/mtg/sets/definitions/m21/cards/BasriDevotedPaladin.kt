package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basri, Devoted Paladin
 * {4}{W}{W}
 * Legendary Planeswalker — Basri
 * Loyalty: 4
 *
 * +1: Put a +1/+1 counter on up to one target creature. It gains vigilance until end of turn.
 * −1: Whenever a creature attacks this turn, put a +1/+1 counter on it.
 * −6: Creatures you control get +2/+2 and gain flying until end of turn.
 *
 * The −1 installs a turn-bounded, repeating delayed trigger (Harald Unites the Elves' chapter III
 * shape): it fans out once per declared attacker — any creature, not only yours — and puts the
 * counter on that attacker (`TriggeringEntity`), resolving before blockers are declared (ruling).
 * Being a delayed trigger, it keeps working even if Basri leaves the battlefield.
 * The −6 is a one-shot over the creatures you control as it resolves (ruling), so creatures you
 * gain later in the turn don't get the bonus.
 */
val BasriDevotedPaladin = card("Basri, Devoted Paladin") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Basri"
    startingLoyalty = 4
    oracleText = "+1: Put a +1/+1 counter on up to one target creature. It gains vigilance until end of turn.\n" +
        "−1: Whenever a creature attacks this turn, put a +1/+1 counter on it.\n" +
        "−6: Creatures you control get +2/+2 and gain flying until end of turn."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature)
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity),
            trigger = Triggers.a(GameObjectFilter.Creature).attacks(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "320"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2688b45d-9d74-4d9f-9a63-29c82b48d64f.jpg?1783930624"
        ruling("2020-06-23", "After activating Basri's second ability, a creature that attacks gets a +1/+1 counter before blockers are chosen.")
        ruling("2020-06-23", "Basri's last ability affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't get +2/+2 or gain flying.")
    }
}
