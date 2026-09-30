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
 * Necrosquito
 * {3}{B}
 * Creature — Phyrexian Insect
 * 0/0
 *
 * Flying
 * This creature enters with two oil counters on it.
 * This creature gets +1/+1 for each oil counter on it.
 * Whenever another creature or artifact you control is put into a graveyard from the battlefield,
 * put an oil counter on this creature.
 */
val Necrosquito = card("Necrosquito") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Insect"
    power = 0
    toughness = 0
    oracleText = "Flying\n" +
        "This creature enters with two oil counters on it.\n" +
        "This creature gets +1/+1 for each oil counter on it.\n" +
        "Whenever another creature or artifact you control is put into a graveyard from the " +
        "battlefield, put an oil counter on this creature."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    staticAbility {
        val oil = DynamicAmounts.countersOnSelf(CounterType.OIL)
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = oil,
            toughnessBonus = oil
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrArtifact.youControl()).dies()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Abz J Harding"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72af72d2-5995-4cad-82f1-e2d0c465d6f1.jpg?1783918044"
    }
}
