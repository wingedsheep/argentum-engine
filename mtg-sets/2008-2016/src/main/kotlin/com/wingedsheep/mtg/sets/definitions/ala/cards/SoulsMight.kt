package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soul's Might — Shards of Alara #149 (canonical / earliest real printing)
 * {4}{G} · Sorcery
 *
 * Put X +1/+1 counters on target creature, where X is that creature's power.
 *
 * X is the target's (projected) power read at resolution.
 */
val SoulsMight = card("Soul's Might") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put X +1/+1 counters on target creature, where X is that creature's power."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.powerOf(creature),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Kev Walker"
        flavorText = "An avatar he sculpts of instinct and force."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77ef536d-d55b-447a-b6ab-04123f1cfaa3.jpg?1783942550"
    }
}
