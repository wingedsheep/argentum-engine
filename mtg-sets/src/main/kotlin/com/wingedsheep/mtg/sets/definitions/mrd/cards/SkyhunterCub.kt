package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Skyhunter Cub — Mirrodin #21
 * {2}{W} · Creature — Cat Knight · 2/2
 *
 * As long as this creature is equipped, it gets +1/+1 and has flying.
 *
 * Two self-scoped statics behind the same "is equipped" gate ([Conditions.SourceMatches] over
 * `GameObjectFilter.Any.equipped()`, as on Merry, Esquire of Rohan). They land in different layers —
 * the keyword grant in layer 6, the stat bump in layer 7c — so they're modelled separately rather
 * than as one bundled ability; the condition is re-read each time state is projected, so unattaching
 * the Equipment drops both halves at once.
 */
val SkyhunterCub = card("Skyhunter Cub") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Knight"
    power = 2
    toughness = 2
    oracleText = "As long as this creature is equipped, it gets +1/+1 and has flying."

    staticAbility {
        ability = GrantKeyword(keyword = Keyword.FLYING.name, filter = GroupFilter.source())
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
    }

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source())
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Pete Venters"
        flavorText = "Every young leonin wishes to become a skyhunter, for they soar closest to the suns."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd46bfba-0685-4dcb-9b63-f90da8fb0ce7.jpg?1783944559"
    }
}
