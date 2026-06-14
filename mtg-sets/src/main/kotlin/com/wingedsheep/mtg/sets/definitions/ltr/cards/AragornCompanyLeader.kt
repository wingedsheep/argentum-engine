package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Aragorn, Company Leader
 * {1}{G}{W}
 * Legendary Creature — Human Ranger
 * 3/3
 *
 * Whenever the Ring tempts you, if you chose a creature other than Aragorn as your Ring-bearer,
 * put your choice of a counter from among first strike, vigilance, deathtouch, and lifelink on
 * Aragorn.
 * Whenever you put one or more counters on Aragorn, put one of each of those kinds of counters on
 * up to one other target creature.
 *
 * The first ability is the Ring-tempt payoff (`Triggers.RingTemptsYou` +
 * `Conditions.YouChoseOtherCreatureAsRingBearer`); "put your choice of a counter from among …" is a
 * resolution-time `Effects.ChooseAction` over the four keyword-counter kinds, each branch adding one
 * counter of that kind to Aragorn (`EffectTarget.Self`). The four counters are keyword counters
 * (CR 122.1d): a first strike / vigilance / deathtouch / lifelink counter grants the matching
 * keyword via the state projection's keyword-counter map.
 *
 * The second ability fires on `Triggers.CountersPlacedOnThis` (SELF-bound `CountersPlacedEvent`, any
 * kind), so any counter landing on Aragorn — including the one from his own first ability — puts one
 * of EACH of the four named kinds onto up to one OTHER target creature
 * (`TargetOther(TargetCreature(optional))`).
 */
val AragornCompanyLeader = card("Aragorn, Company Leader") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Ranger"
    power = 3
    toughness = 3
    oracleText = "Whenever the Ring tempts you, if you chose a creature other than Aragorn as your " +
        "Ring-bearer, put your choice of a counter from among first strike, vigilance, deathtouch, " +
        "and lifelink on Aragorn.\n" +
        "Whenever you put one or more counters on Aragorn, put one of each of those kinds of " +
        "counters on up to one other target creature."

    triggeredAbility {
        trigger = Triggers.RingTemptsYou
        triggerCondition = Conditions.YouChoseOtherCreatureAsRingBearer
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "First strike counter",
                    effect = Effects.AddCounters(Counters.FIRST_STRIKE, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "Vigilance counter",
                    effect = Effects.AddCounters(Counters.VIGILANCE, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "Deathtouch counter",
                    effect = Effects.AddCounters(Counters.DEATHTOUCH, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "Lifelink counter",
                    effect = Effects.AddCounters(Counters.LIFELINK, 1, EffectTarget.Self),
                ),
            )
        )
        description = "Whenever the Ring tempts you, if you chose a creature other than Aragorn as " +
            "your Ring-bearer, put your choice of a counter from among first strike, vigilance, " +
            "deathtouch, and lifelink on Aragorn."
    }

    triggeredAbility {
        trigger = Triggers.CountersPlacedOnThis
        target("up to one other target creature", TargetOther(TargetCreature(count = 1, minCount = 0, optional = true)))
        effect = Effects.Composite(
            listOf(
                Effects.AddCounters(Counters.FIRST_STRIKE, 1, EffectTarget.ContextTarget(0)),
                Effects.AddCounters(Counters.VIGILANCE, 1, EffectTarget.ContextTarget(0)),
                Effects.AddCounters(Counters.DEATHTOUCH, 1, EffectTarget.ContextTarget(0)),
                Effects.AddCounters(Counters.LIFELINK, 1, EffectTarget.ContextTarget(0)),
            )
        )
        description = "Whenever you put one or more counters on Aragorn, put one of each of those " +
            "kinds of counters on up to one other target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73ca5f48-0117-49d6-8b00-b8482e3545b3.jpg?1686969633"
    }
}
