package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oneirophage
 * {3}{U}
 * Creature — Squid Illusion
 * 1/2
 *
 * Flying
 * Whenever you draw a card, put a +1/+1 counter on this creature.
 *
 * `Triggers.you.draws()` fires once per card drawn, so a multi-card draw stacks that many
 * counters; cards put into hand without the word "draw" don't trigger it (2019-06-14 ruling).
 */
val Oneirophage = card("Oneirophage") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Squid Illusion"
    power = 1
    toughness = 2
    oracleText = "Flying\nWhenever you draw a card, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Martina Pilcerova"
        flavorText = "It manifests at wizard academies to siphon inspiration from young prodigies."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caefbac6-b2a0-4a70-a912-25173eafd7b3.jpg?1783933142"

        ruling("2019-06-14", "If a spell or ability causes you to put cards into your hand without specifically using the word \"draw,\" Oneirophage's ability won't trigger.")
    }
}
