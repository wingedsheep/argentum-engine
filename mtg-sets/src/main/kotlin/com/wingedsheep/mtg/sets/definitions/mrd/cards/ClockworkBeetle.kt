package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clockwork Beetle — Mirrodin #153
 * {1} · Artifact Creature — Insect · 0/0
 *
 * This creature enters with two +1/+1 counters on it.
 * Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat.
 *
 * Modelling notes:
 * - Base P/T is the printed 0/0; the two +1/+1 counters (CR 613.4c, layer 7d) make it a 2/2 on
 *   the battlefield. It dies as a state-based action once the last counter is shed.
 * - The printed ability is a trigger that *sets up a delayed trigger* ("…remove a counter from
 *   it at end of combat"). It is modelled the way its Antiquities ancestor Clockwork Avian
 *   already is in this codebase: an [Triggers.EachEndOfCombat] trigger with the intervening-if
 *   [Conditions.SourceAttackedOrBlockedThisCombat]. That is observationally identical — one
 *   counter shed per combat the Beetle fought in, on any player's turn — because the delayed
 *   trigger and the tracker are keyed to the same object and both go away when it leaves the
 *   battlefield.
 */
val ClockworkBeetle = card("Clockwork Beetle") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Insect"
    power = 0
    toughness = 0
    oracleText = "This creature enters with two +1/+1 counters on it.\n" +
        "Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 2,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.EachEndOfCombat
        triggerCondition = Conditions.SourceAttackedOrBlockedThisCombat
        effect = Effects.RemoveCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc3c8db7-cf58-4571-b4ad-2b68d73495b2.jpg?1783944525"
    }
}
