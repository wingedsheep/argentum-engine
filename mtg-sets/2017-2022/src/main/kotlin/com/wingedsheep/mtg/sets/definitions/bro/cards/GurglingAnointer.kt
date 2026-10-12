package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gurgling Anointer
 * {2}{B}
 * Creature — Phyrexian Horror
 * 1/3
 * Flying
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 * When this creature dies, return another target creature card with mana value less than or
 * equal to this creature's power from your graveyard to the battlefield.
 *
 * Faerie Vandal's draw-second trigger plus Fiendish Panda's dies-reanimation: the mana-value cap
 * reads the source's power via [DynamicAmounts.sourcePower], which falls back to last-known
 * power once this creature is in the graveyard; `.other()` keeps it from returning itself.
 */
val GurglingAnointer = card("Gurgling Anointer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Horror"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, return another target creature card with mana value less than " +
        "or equal to this creature's power from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.sourcePower()),
                zone = Zone.GRAVEYARD,
            ).other(),
        )
        effect = Effects.PutOntoBattlefield(t)
        description = "When this creature dies, return another target creature card with mana value " +
            "less than or equal to this creature's power from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ce0d139-2bea-48df-a007-cee7c8ce07bd.jpg?1783920086"
    }
}
