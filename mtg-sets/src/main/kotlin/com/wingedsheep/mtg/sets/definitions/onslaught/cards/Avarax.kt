package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Avarax
 * {3}{R}{R}
 * Creature — Beast
 * 3/3
 * Haste
 * When Avarax enters the battlefield, you may search your library for a card
 * named Avarax, reveal it, put it into your hand, then shuffle.
 * {1}{R}: Avarax gets +1/+0 until end of turn.
 */
val Avarax = card("Avarax") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Haste\nWhen Avarax enters the battlefield, you may search your library for a card named Avarax, reveal it, put it into your hand, then shuffle.\n{1}{R}: Avarax gets +1/+0 until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.Any.named("Avarax"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = ModifyStatsEffect(
            powerModifier = 1,
            toughnessModifier = 0,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/large/front/a/e/ae76705f-ec95-48b0-9e26-84ce40c9514b.jpg?1562936224"
    }
}
