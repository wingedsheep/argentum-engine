package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Prismatic Undercurrents
 * {3}{G}
 * Enchantment
 *
 * Vivid — When this enchantment enters, search your library for up to X basic land cards,
 * where X is the number of colors among permanents you control. Reveal those cards,
 * put them into your hand, then shuffle.
 * You may play an additional land on each of your turns.
 */
val PrismaticUndercurrents = card("Prismatic Undercurrents") {
    manaCost = "{3}{G}"
    typeLine = "Enchantment"

    // Vivid — When this enchantment enters, search your library for up to X basic land cards,
    // where X is the number of colors among permanents you control. Reveal those cards,
    // put them into your hand, then shuffle.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.searchLibrary(
            filter = Filters.BasicLand,
            count = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                aggregation = Aggregation.DISTINCT_COLORS
            ),
            reveal = true
        )
    }

    // You may play an additional land on each of your turns.
    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Steve Ellis"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb7490f2-1425-495f-b7d4-2f1e0df7490e.jpg?1767952219"
        ruling(
            "2025-11-17",
            "The value of X is calculated only once, as Prismatic Undercurrents's first ability resolves."
        )
        ruling(
            "2025-11-17",
            "The effect of Prismatic Undercurrents's last ability is cumulative with similar effects. For example, if you control two Prismatic Undercurrents, you'll be able to play three lands during each of your turns."
        )
    }
}
