package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Craig Boone, Novac Guard — Fallout #100.
 *
 * "Put two quest counters on Craig Boone. When you do, …" — the reflexive trigger fires once for
 * every two quest counters actually placed (the 2024-06-07 ruling: a Doubling Season makes it
 * trigger twice, and a Craig Boone that left the battlefield gets no counters and no trigger).
 * So the count is read around the placement — counters after minus counters before, halved —
 * and that many reflexive triggers are emitted, each choosing its own up-to-one target as it
 * goes on the stack.
 *
 * The damage is read when the reflexive ability resolves, falling back to last-known counters
 * if Craig Boone has left (the 2024-03-08 ruling). The targeted creature's controller chooses
 * whether Craig Boone damages them instead; with no target chosen, nothing happens.
 */
val CraigBooneNovacGuard = card("Craig Boone, Novac Guard") {
    manaCost = "{1}{R}{W}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Reach, lifelink\nOne for My Baby — Whenever you attack with two or more creatures, put two quest counters on Craig Boone. When you do, Craig Boone deals damage equal to the number of quest counters on it to up to one target creature unless that creature's controller has Craig Boone deal that much damage to them."
    keywords(Keyword.REACH, Keyword.LIFELINK)
    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 2)
        val questCounters = DynamicAmounts.countersOnSelf(CounterType.QUEST)
        val whenYouDo = Effects.ReflexiveTrigger(
            action = Effects.Nothing,
            optional = false,
            descriptionOverride = "Craig Boone deals damage equal to the number of quest counters on it to up to " +
                "one target creature unless that creature's controller has Craig Boone deal that much damage to them.",
        ) {
            val creature = target(TargetFilter.Creature, optional = true)
            effect = Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature, creature),
                Effects.May(
                    effect = Effects.DealDamage(questCounters, EffectTarget.TargetController, damageSource = EffectTarget.Self),
                    otherwise = Effects.DealDamage(questCounters, creature, damageSource = EffectTarget.Self),
                    decisionMaker = EffectTarget.TargetController,
                    prompt = "Have Craig Boone deal that much damage to you instead of to your creature?",
                ),
            )
        }
        effect = Effects.Pipeline(
            descriptionOverride = "Put two quest counters on Craig Boone. When you do, Craig Boone deals damage " +
                "equal to the number of quest counters on it to up to one target creature unless that creature's " +
                "controller has Craig Boone deal that much damage to them.",
        ) {
            val before = storeNumber(questCounters)
            run(Effects.AddCounters(CounterType.QUEST, 2, EffectTarget.Self))
            run(Effects.Repeat((questCounters - before.amount) / 2, whenYouDo))
        }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Pierre Raveneau"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63d83dad-21c3-4777-84b0-3c775585b5fd.jpg?1783912390"
        ruling("2024-06-07", "In the rare case where more than two quest counters are placed on Craig Boone because of the effect of its last ability (for example, because of the effect of Doubling Season), the reflexive triggered ability will trigger once for every two quest counters placed on Craig Boone this way.")
        ruling("2024-03-08", "You don't choose a target for Craig Boone's last ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you put two quest counters on Craig Boone this way. You choose up to one target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
        ruling("2024-03-08", "If Craig Boone is no longer on the battlefield when the reflexive triggered ability resolves, use the number of quest counters that were on it when it was last on the battlefield to determine how much damage it deals.")
    }
}
