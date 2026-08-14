package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clockwork Condor — Mirrodin #154
 * {4} · Artifact Creature — Bird · 0/0
 *
 * Flying
 * This creature enters with three +1/+1 counters on it.
 * Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat.
 *
 * The Mirrodin Clockwork Beetle with one more counter and a pair of wings, so it is modelled the
 * same way its setmate and its Antiquities ancestor Clockwork Avian already are: the printed 0/0
 * plus three +1/+1 counters (CR 613.4c, layer 7d) make a 3/3 on the battlefield, and it dies as a
 * state-based action once the last counter is shed.
 *
 * The printed ability is a trigger that *sets up a delayed trigger* ("…remove a counter from it at
 * end of combat"). [Triggers.EachEndOfCombat] with the intervening-if
 * [Conditions.SourceAttackedOrBlockedThisCombat] is observationally identical — one counter shed
 * per combat the Condor fought in, on any player's turn — because the delayed trigger and the
 * tracker are keyed to the same object and both go away when it leaves the battlefield.
 */
val ClockworkCondor = card("Clockwork Condor") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Bird"
    power = 0
    toughness = 0
    oracleText = "Flying\n" +
        "This creature enters with three +1/+1 counters on it.\n" +
        "Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 3,
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
        collectorNumber = "154"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02a7e1a6-347e-47bc-8a14-e584a45941e1.jpg?1783944525"
    }
}
