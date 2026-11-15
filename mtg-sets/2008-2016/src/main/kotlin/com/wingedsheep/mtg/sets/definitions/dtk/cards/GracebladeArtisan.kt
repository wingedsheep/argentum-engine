package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Graceblade Artisan — Dragons of Tarkir #20
 * {2}{W} · Creature — Human Monk · 2/3
 *
 * This creature gets +2/+2 for each Aura attached to it.
 *
 * Rabid Wombat's shape: [DynamicAmounts.aurasAttachedToSelf] counts only Auras (Equipment
 * doesn't feed it) and is recomputed continuously, so the bonus changes the moment an Aura
 * is attached or leaves.
 */
val GracebladeArtisan = card("Graceblade Artisan") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 3
    oracleText = "This creature gets +2/+2 for each Aura attached to it."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.aurasAttachedToSelf() * 2,
            toughnessBonus = DynamicAmounts.aurasAttachedToSelf() * 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Magali Villeneuve"
        flavorText = "\"Can you catch a snowflake on the edge of your blade? Her sword has snagged an entire blizzard.\"\n—Zogye, Ojutai mystic"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eedcf6c-5796-4843-b6b8-df78e11e72c2.jpg?1783938616"
    }
}
