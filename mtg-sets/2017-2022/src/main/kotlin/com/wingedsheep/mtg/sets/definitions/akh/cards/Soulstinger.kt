package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soulstinger
 * {3}{B}
 * Creature — Scorpion Demon
 * 4/5
 * When this creature enters, put two -1/-1 counters on target creature you control.
 * When this creature dies, you may put a -1/-1 counter on target creature for each -1/-1 counter
 * on this creature.
 *
 * The dies trigger resolves after Soulstinger has left the battlefield, so its -1/-1 count is read
 * from the leaves-the-battlefield snapshot (`lastKnownSourceCounters`, CR 608.2h) — counting only the
 * -1/-1 kind. The printed "you may" is the resolution-time yes/no (`optional = true`).
 */
val Soulstinger = card("Soulstinger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Scorpion Demon"
    oracleText = "When this creature enters, put two -1/-1 counters on target creature you control.\n" +
        "When this creature dies, you may put a -1/-1 counter on target creature for each -1/-1 counter on this creature."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 2, creature)
        description = "When this creature enters, put two -1/-1 counters on target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.Creature)
        optional = true
        effect = Effects.AddDynamicCounters(
            CounterType.MINUS_ONE_MINUS_ONE,
            DynamicAmounts.lastKnownSourceCounters(CounterType.MINUS_ONE_MINUS_ONE),
            creature
        )
        description = "When this creature dies, you may put a -1/-1 counter on target creature for each -1/-1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Mike Burns"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0504be31-eaab-4b3e-8cfb-274e46d3d67a.jpg?1783936499"
    }
}
