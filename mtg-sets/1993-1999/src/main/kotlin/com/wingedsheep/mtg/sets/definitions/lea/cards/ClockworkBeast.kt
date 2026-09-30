package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ClockworkBeast = card("Clockwork Beast") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Beast"
    power = 0
    toughness = 4
    oracleText = "This creature enters with seven +1/+0 counters on it.\nAt end of combat, if this creature attacked or blocked this combat, remove a +1/+0 counter from it.\n{X}, {T}: Put up to X +1/+0 counters on this creature. This ability can't cause the total number of +1/+0 counters on this creature to be greater than seven. Activate only during your upkeep."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ZERO,
        count = 7,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT)
        interveningIf = Conditions.SourceAttackedOrBlockedThisCombat
        effect = Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ZERO, 1, EffectTarget.Self)
        description = "At end of combat, if this creature attacked or blocked this combat, remove a +1/+0 counter from it."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        effect = Effects.If(
            condition = Conditions.SourceMatches(GameObjectFilter.Permanent.sourceItself().currentlyIn(Zone.BATTLEFIELD)),
            then = Effects.ChooseNumberThen(
                maxValue = DynamicAmounts.nonNegative(DynamicAmounts.min(
                    DynamicAmounts.xValue(),
                    7 - DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ZERO)
                )),
                prompt = "Put how many +1/+0 counters on Clockwork Beast?",
                then = Effects.AddCountersWithLimit(
                    CounterType.PLUS_ONE_PLUS_ZERO,
                    DynamicAmounts.xValue(),
                    DynamicAmounts.fixed(7),
                    EffectTarget.Self
                )
            )
        )
        restrictions = listOf(
            ActivationRestriction.DuringStep(Step.UPKEEP),
            ActivationRestriction.OnlyDuringYourTurn
        )
        description = "{X}, {T}: Put up to X +1/+0 counters on this creature (max seven total). Activate only during your upkeep."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f916a2-0ace-44b5-99dc-72979af34db9.jpg?1783948668"
        ruling("2007-09-16", "This is a change from the most recent wording. Now, if some other spell or ability causes +1/+0 counters to be put on Clockwork Beast, it can wind up with more than seven such counters on it.")
        ruling("2007-09-16", "If Clockwork Beast has seven or fewer +1/+0 counters on it when its last ability resolves, it can wind up a maximum of seven such counters on it. If it has seven or more +1/+0 counters on it, the ability will have no effect.")
        ruling("2007-09-16", "Clockwork Beast’s last ability resolves, you can choose to put fewer than X +1/+0 counters on it.")
        ruling("2004-10-04", "Loses a counter even if it is affected by a Fog-like effect which prevents it from dealing damage.")
        ruling("2004-10-04", "Can attack or block even if it has no counters.")
    }
}
