package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trawler Drake — Phyrexia: All Will Be One #74
 * {2}{U} · Creature — Phyrexian Drake · 0/0 · Uncommon
 *
 * Flying
 * This creature enters with an oil counter on it.
 * This creature gets +1/+1 for each oil counter on it.
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 *
 * Evolving Adaptive's oil-counter body plus Atmosphere Surgeon's noncreature-cast trigger.
 */
val TrawlerDrake = card("Trawler Drake") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Drake"
    power = 0
    toughness = 0
    oracleText = "Flying\n" +
        "This creature enters with an oil counter on it.\n" +
        "This creature gets +1/+1 for each oil counter on it.\n" +
        "Whenever you cast a noncreature spell, put an oil counter on this creature."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 1, selfOnly = true))

    staticAbility {
        val oil = DynamicAmounts.countersOnSelf(CounterType.OIL)
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = oil,
            toughnessBonus = oil
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Whenever you cast a noncreature spell, put an oil counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f442442d-2e5d-4913-82d8-bab2f31541f8.jpg?1783918055"
    }
}
