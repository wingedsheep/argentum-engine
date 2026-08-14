package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clockwork Vorrac — Mirrodin #156
 * {5} · Artifact Creature — Boar Beast · 0/0
 *
 * Trample
 * This creature enters with four +1/+1 counters on it.
 * Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat.
 * {T}: Put a +1/+1 counter on this creature.
 *
 * Modelling notes:
 * - Base P/T is the printed 0/0; the four +1/+1 counters (CR 613.4c, layer 7d) make it a 4/4 on
 *   the battlefield, and it dies as a state-based action once the last counter is shed.
 * - The counter-shed line follows the same shape as its set-mate [ClockworkBeetle] and their
 *   Antiquities ancestor Clockwork Avian: a [Triggers.EachEndOfCombat] trigger with the
 *   intervening-if [Conditions.SourceAttackedOrBlockedThisCombat], which is observationally
 *   identical to the printed "set up a delayed trigger" wording — one counter shed per combat the
 *   Vorrac fought in, on any player's turn.
 * - Unlike Clockwork Avian, the refill ability carries **no cap and no timing restriction**: it is
 *   a bare {T} ability usable at instant speed on any turn, so the counters can climb past four.
 *   Because it *is* the {T} symbol in the ability's own cost, summoning sickness does gate it
 *   (CR 302.6) and it can only be used once per untap.
 */
val ClockworkVorrac = card("Clockwork Vorrac") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Boar Beast"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "This creature enters with four +1/+1 counters on it.\n" +
        "Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat.\n" +
        "{T}: Put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 4,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.EachEndOfCombat
        triggerCondition = Conditions.SourceAttackedOrBlockedThisCombat
        effect = Effects.RemoveCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{T}: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e876938-1b8e-44cf-ade2-a42f8acdf24c.jpg?1783944525"
    }
}
