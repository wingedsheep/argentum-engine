package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foe-Razer Regent
 * {5}{G}{G}
 * Creature — Dragon
 * 4/5
 * Flying
 * When this creature enters, you may have it fight target creature you don't control.
 * Whenever a creature you control fights, put two +1/+1 counters on it at the beginning of the
 * next end step.
 *
 * The fight payoff watches `Triggers.a(creature you control).fights()` — once per fighter — and
 * schedules a delayed end-step trigger bound to that creature's identity, so a fighter that leaves
 * the battlefield before the end step gets nothing (and a fight during the end step waits for the
 * next turn's end step).
 */
val FoeRazerRegent = card("Foe-Razer Regent") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying\nWhen this creature enters, you may have it fight target creature you don't control.\n" +
        "Whenever a creature you control fights, put two +1/+1 counters on it at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Fight(EffectTarget.Self, t)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).fights()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a266c3-de47-4e94-bdc4-210e34981de3.jpg?1783938580"
    }
}
