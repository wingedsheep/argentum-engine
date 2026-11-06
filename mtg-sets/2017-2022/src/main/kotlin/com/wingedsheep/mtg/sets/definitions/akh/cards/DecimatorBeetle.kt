package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decimator Beetle — Amonkhet #197 (canonical printing)
 * {3}{B}{G} · Creature — Insect · 4/5
 *
 * When this creature enters, put a -1/-1 counter on target creature you control.
 * Whenever this creature attacks, remove a -1/-1 counter from target creature you control and put
 * a -1/-1 counter on up to one target creature defending player controls.
 *
 * Per the rulings, the attack trigger's first target needn't carry a -1/-1 counter: removing from a
 * creature with none is a no-op and the opposing creature still gets its counter. Each target is
 * checked independently on resolution, so one becoming illegal doesn't stop the other half.
 */
val DecimatorBeetle = card("Decimator Beetle") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Insect"
    oracleText = "When this creature enters, put a -1/-1 counter on target creature you control.\n" +
        "Whenever this creature attacks, remove a -1/-1 counter from target creature you control and put a -1/-1 counter on up to one target creature defending player controls."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
        description = "When this creature enters, put a -1/-1 counter on target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.Creature.defendingPlayerControls(), optional = true)
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, mine) then
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, theirs)
        description = "Whenever this creature attacks, remove a -1/-1 counter from target creature you control " +
            "and put a -1/-1 counter on up to one target creature defending player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Adrian Majkrzak"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b75e834-fa46-4833-ae9f-1bc2809ece2f.jpg?1783936464"
    }
}
