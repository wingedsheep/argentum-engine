package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nykthos Paragon — Modern Horizons 2 #22
 * {4}{W}{W}
 * Enchantment Creature — Human Soldier
 * 4/6
 *
 * Whenever you gain life, you may put that many +1/+1 counters on each creature you control.
 * Do this only once each turn.
 *
 * "That many" is the triggering life-gain event's amount ([DynamicAmounts.triggerLifeGained]),
 * read per iteration from the trigger context. "Do this only once each turn" is
 * `effectOncePerTurn` (CR 603.2h), keyed to the *action*, not the trigger: declining leaves the
 * turn's use unspent so the next life gain offers it again, and once taken, later instances on the
 * stack do nothing and further life gain no longer triggers it (2021-06-18 rulings). The
 * trigger-level `oncePerTurn` cap would wrongly burn the turn on a declined first gain.
 */
val NykthosParagon = card("Nykthos Paragon") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Soldier"
    power = 4
    toughness = 6
    oracleText = "Whenever you gain life, you may put that many +1/+1 counters on each creature you control. " +
        "Do this only once each turn."

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effectOncePerTurn = true
        effect = Effects.May(
            Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddDynamicCounters(
                    counterType = CounterType.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmounts.triggerLifeGained(),
                    target = EffectTarget.IterationEntity,
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Martina Pilcerova"
        flavorText = "Though Heliod turned against humanity in his hunger for power, the ideals he once championed still stand strong."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2963205e-b181-44d1-809f-6577e29fa812.jpg?1783926889"
        ruling("2021-06-18", "\"Do this only once each turn\" refers to putting +1/+1 counters on your creatures using the triggered ability. As long as you haven't yet chosen to put +1/+1 counters on your creatures with it, all instances of gaining life will cause Nykthos Paragon's ability to trigger.")
        ruling("2021-06-18", "\"Do this only once each turn\" refers only to the ability it is attached to, not other instances of the same ability. In other words, if you control multiple Nykthos Paragons, you will be able to do this once for each of them.")
        ruling("2021-06-18", "If multiple instances of the ability are on the stack, you will be able to put +1/+1 counters for only one of those instances. Once you do that, other instances will do nothing as they resolve.")
    }
}
