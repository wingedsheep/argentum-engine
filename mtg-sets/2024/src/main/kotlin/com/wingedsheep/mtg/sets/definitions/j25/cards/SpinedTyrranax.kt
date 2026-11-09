package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spined Tyrranax — Foundations Jumpstart #26 (canonical printing)
 * {4}{G} · Creature — Dinosaur Beast · 5/5
 *
 * At the beginning of combat on your turn, you may pay {2}{G}. When you do, put a +1/+1 counter on
 * target creature. That creature gains trample until end of turn.
 *
 * "When you do" is a reflexive trigger (Skyrider Patrol's shape): {2}{G} is paid as the
 * beginning-of-combat trigger resolves, and only then does the counter-and-trample ability go on
 * the stack and choose its target — any creature, not just one you control.
 */
val SpinedTyrranax = card("Spined Tyrranax") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur Beast"
    power = 5
    toughness = 5
    oracleText = "At the beginning of combat on your turn, you may pay {2}{G}. When you do, put a +1/+1 " +
        "counter on target creature. That creature gains trample until end of turn. (It can deal excess " +
        "combat damage to the player or planeswalker it's attacking.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ReflexiveTrigger(action = Effects.PayMana("{2}{G}")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Brent Hollowell"
        flavorText = "\"Drive the beasts into the enemy ranks, then stay back and let them do the work.\"\n—Koth"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb589caa-bba4-4d23-bdfe-7fc37f19bfd6.jpg?1783908862"
        ruling(
            "2024-11-08",
            "You don't choose a target for Spined Tyrranax's ability at the time it triggers. Rather, a second " +
                "\"reflexive\" ability triggers when you pay {2}{G} this way. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal.",
        )
    }
}
