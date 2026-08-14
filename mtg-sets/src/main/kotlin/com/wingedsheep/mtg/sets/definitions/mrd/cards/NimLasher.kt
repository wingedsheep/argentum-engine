package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Nim Lasher — Mirrodin #71
 * {2}{B} · Creature — Zombie · 1/1
 *
 * This creature gets +1/+0 for each artifact you control.
 *
 * A continuously recomputed [GrantDynamicStatsEffect] on the Nim itself ([GroupFilter.source]):
 * power tracks the controller's artifact count in real time, so an artifact entering or leaving
 * moves the Nim's power immediately rather than snapshotting on entry. Toughness is untouched
 * ([DynamicAmount.Fixed] 0).
 *
 * Nim Lasher is *not* itself an artifact, so it never counts itself — with no artifacts out it
 * attacks as a plain 1/1.
 */
val NimLasher = card("Nim Lasher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+0 for each artifact you control."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Adam Rex"
        flavorText = "The rotting metal feeds the necrogen mists, and in turn the mists feed the nim."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b66c698-02bd-48ef-a866-5251bdc02c16.jpg?1783944546"
    }
}
