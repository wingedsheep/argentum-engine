package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Civic Saber — Return to Ravnica #227
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0 for each of its colors.
 * Equip {1}
 *
 * A [GrantDynamicStats] over the equipped creature whose power bonus counts the colors of that
 * creature ([DynamicAmounts.colorCountOf] on [EffectTarget.EquippedCreature], resolved through
 * the Saber's attachment link). The count reads projected colors, so a color-changing effect on
 * the creature changes the bonus; a colorless creature gets +0/+0.
 */
val CivicSaber = card("Civic Saber") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 for each of its colors.\n" +
        "Equip {1}"

    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.colorCountOf(EffectTarget.EquippedCreature),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29c9247e-05f4-44bb-86e3-90a60e880374.jpg?1783940324"
    }
}
