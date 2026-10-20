package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Part Water — Legends #66
 * {X}{X}{U} · Sorcery
 *
 * X target creatures gain islandwalk until end of turn.
 *
 * "X target creatures" is exactly X (`targets(…, exactly = X)`), the same shape as Word of
 * Binding; each target is granted islandwalk on its own, so an illegal target at resolution
 * simply misses out while the rest still gain it.
 */
val PartWater = card("Part Water") {
    manaCost = "{X}{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "X target creatures gain islandwalk until end of turn. " +
        "(They can't be blocked as long as defending player controls an Island.)"

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.GrantKeyword(Keyword.ISLANDWALK, EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "NéNé Thomas"
        flavorText = "\". . . and the waters were a wall unto them on their right hand, and on their left.\"\n—Exodus, 14:22"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b659475-c8b7-493d-af63-04f34d8cc3b1.jpg?1783948074"
    }
}
