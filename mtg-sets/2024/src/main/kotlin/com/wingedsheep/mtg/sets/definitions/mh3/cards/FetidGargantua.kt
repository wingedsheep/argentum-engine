package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fetid Gargantua {4}{B}
 * Creature — Horror
 * 4/4
 * {2}{B}: Adapt 2.
 * Whenever one or more +1/+1 counters are put on this creature, you may draw two cards. If you do,
 * you lose 2 life.
 *
 * Adapt is the zero-counter gate over AddCounters (CR 701.46a). The "if you do" hangs only on the
 * optional draw, so the draw and the life loss sit together inside one [Effects.May].
 */
val FetidGargantua = card("Fetid Gargantua") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    oracleText = "{2}{B}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, you may draw two cards. If you do, you lose 2 life."
    power = 4
    toughness = 4

    // {2}{B}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.May(
            Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Michael C. Hayes"
        flavorText = "When the night comes knocking\n—Kessig expression meaning \"in dire times\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cd368a9-5efe-4755-89be-a05cfa2b71c0.jpg?1783911281"
    }
}
