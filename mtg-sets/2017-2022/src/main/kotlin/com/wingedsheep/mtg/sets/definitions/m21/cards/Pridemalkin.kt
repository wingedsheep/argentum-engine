package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pridemalkin — Core Set 2021 #196
 * {2}{G} · Creature — Cat · 2 / 1
 *
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * Each creature you control with a +1/+1 counter on it has trample.
 */
val Pridemalkin = card("Pridemalkin") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "Each creature you control with a +1/+1 counter on it has trample. (It can deal excess " +
        "combat damage to the player or planeswalker it's attacking.)"

    // When this creature enters, put a +1/+1 counter on target creature you control.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    // Each creature you control with a +1/+1 counter on it has trample.
    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df520254-0c72-496b-9222-263ca9d3c5d5.jpg?1783930671"
    }
}
