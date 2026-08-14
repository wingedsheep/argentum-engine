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
 * Leonin Den-Guard — Mirrodin #9
 * {1}{W} · Creature — Cat Soldier · 1/3
 *
 * As long as this creature is equipped, it gets +1/+1 and has vigilance.
 *
 * Two statics sharing one condition rather than one compound ability, because the halves land in
 * different Rule 613 layers: [ModifyStats] applies in layer 7c and [GrantKeyword] in layer 6, and
 * splitting them lets each sort into its own layer without a bespoke multi-layer type.
 *
 * The gate is [Conditions.SourceMatches] over `equipped()`, evaluated during static-ability
 * projection — so attaching or removing Equipment flips both halves immediately, with no trigger
 * and no timestamp of its own.
 */
val LeoninDenGuard = card("Leonin Den-Guard") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 1
    toughness = 3
    oracleText = "As long as this creature is equipped, it gets +1/+1 and has vigilance."

    val whileEquipped = Conditions.SourceMatches(GameObjectFilter.Any.equipped())

    staticAbility {
        condition = whileEquipped
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source())
    }
    staticAbility {
        condition = whileEquipped
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Todd Lockwood"
        flavorText = "No one under the four suns can elude the watchful eye of the den-guard."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9f30b7a-75bb-44e6-b1a2-726df1b5b1f3.jpg?1783944561"
    }
}
