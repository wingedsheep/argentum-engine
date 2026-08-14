package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Long Goodbye — Murders at Karlov Manor #92
 * {1}{B} · Instant
 *
 * This spell can't be countered. (This includes by the ward ability.)
 * Destroy target creature or planeswalker with mana value 3 or less.
 *
 * `cantBeCountered` stamps `CantBeCounteredComponent` on the spell, which every counter path —
 * including ward, which routes through `StackResolver.counterSpell` — checks before removing it
 * from the stack. So the 2024-02-02 ruling holds: targeting a warded creature still *offers* the
 * ward cost, and declining leaves Long Goodbye on the stack to resolve anyway.
 */
val LongGoodbye = card("Long Goodbye") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell can't be countered. (This includes by the ward ability.)\n" +
        "Destroy target creature or planeswalker with mana value 3 or less."

    cantBeCountered = true

    spell {
        val t = target(
            "target creature or planeswalker with mana value 3 or less",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(3))
            )
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Jarel Threat"
        flavorText = "As the days grew longer, so did the list of missing persons."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3896705-bbd2-4ffb-a590-ee78e0eabdc5.jpg?1783912896"
        ruling("2024-02-02", "If you target a creature or planeswalker with ward, you may still pay the ward cost, but Long Goodbye won't be countered even if you don't.")
    }
}
