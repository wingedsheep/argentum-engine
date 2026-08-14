package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Nim Shrieker — Mirrodin #73
 * {3}{B} · Creature — Zombie · 0/1
 *
 * Flying
 * This creature gets +1/+0 for each artifact you control.
 *
 * The flying member of the Nim cycle, sharing [NimLasher]'s artifact-count power boost — a
 * continuously recomputed [GrantDynamicStatsEffect] on the source itself, toughness untouched.
 *
 * Printed base power is 0, so with no artifacts on the battlefield the Shrieker deals no combat
 * damage at all; it needs at least one artifact before the evasion is worth anything. It is not
 * itself an artifact and never counts toward its own boost.
 */
val NimShrieker = card("Nim Shrieker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 0
    toughness = 1
    oracleText = "Flying\n" +
        "This creature gets +1/+0 for each artifact you control."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Adam Rex"
        flavorText = "As imps they were an annoyance. As nim they are a pestilence."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a2c39b-b302-4cc3-b507-e4fe00614036.jpg?1783944545"
    }
}
