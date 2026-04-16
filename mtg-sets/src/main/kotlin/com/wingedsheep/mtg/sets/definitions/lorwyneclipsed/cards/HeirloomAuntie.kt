package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heirloom Auntie
 * {2}{B}
 * Creature — Goblin Warlock
 * 4/4
 *
 * This creature enters with two -1/-1 counters on it.
 * Whenever another creature you control dies, surveil 1, then remove a -1/-1
 * counter from this creature.
 */
val HeirloomAuntie = card("Heirloom Auntie") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Goblin Warlock"
    power = 4
    toughness = 4
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "Whenever another creature you control dies, surveil 1, then remove a -1/-1 counter from this creature. " +
        "(To surveil 1, look at the top card of your library. You may put it into your graveyard.)"

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = EffectPatterns.surveil(1) then
            Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cac251b2-d2cc-45b6-9ca8-678e0eab56ea.jpg?1767732699"
        ruling(
            "2025-11-17",
            "If Heirloom Auntie dies at the same time as one or more other creatures you control, " +
                "its last ability will trigger for each of those other creatures."
        )
    }
}
