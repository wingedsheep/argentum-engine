package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodcrazed Hoplite — Journey into Nyx #61 (canonical printing)
 * {1}{B}
 * Creature — Human Soldier
 * 2/1
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it.
 * Whenever a +1/+1 counter is put on this creature, remove a +1/+1 counter from target creature
 * an opponent controls.
 *
 * Heroic is the `TargetsSource` cast trigger (Satyr Hoplite). "Whenever **a** +1/+1 counter is put
 * on" is the per-counter template (`perCounter`, Flourishing Defenses): two counters placed at once,
 * or entering with counters, trigger it once per counter, per the rulings.
 */
val BloodcrazedHoplite = card("Bloodcrazed Hoplite") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it.\n" +
        "Whenever a +1/+1 counter is put on this creature, remove a +1/+1 counter from target creature " +
        "an opponent controls."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE, perCounter = true)
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Whenever a +1/+1 counter is put on this creature, remove a +1/+1 counter from " +
            "target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Jeff Simpson"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2a6556b-a4f1-4ff5-8e6e-a47618c8c66e.jpg?1783939439"
        ruling("2014-04-26", "Bloodcrazed Hoplite's last ability will trigger whenever any +1/+1 counter is placed on it, not just ones due to its heroic ability.")
        ruling("2014-04-26", "If multiple +1/+1 counters are placed on Bloodcrazed Hoplite simultaneously, it last ability will trigger once for each of those counters.")
        ruling("2014-04-26", "If Bloodcrazed Hoplite enters the battlefield with +1/+1 counters on it, its last ability will trigger once for each of those counters.")
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
