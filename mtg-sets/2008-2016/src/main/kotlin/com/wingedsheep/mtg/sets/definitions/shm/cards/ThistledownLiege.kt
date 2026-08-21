package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thistledown Liege
 * {1}{W/U}{W/U}{W/U}
 * Creature — Kithkin Knight
 * 1 / 3
 *
 * Flash
 * Other white creatures you control get +1/+1.
 * Other blue creatures you control get +1/+1.
 *
 * - Two deliberately separate [ModifyStats] statics, exactly as on Wilt-Leaf Liege: they stack, so
 *   a creature you control that is both white *and* blue gets +2/+2.
 * - `excludeSelf = true` carries the printed "Other" — the Liege is white *and* blue itself, so
 *   without it it would pump itself twice.
 * - The colour test reads projected state through the [GroupFilter], so a creature that only
 *   becomes white or blue from a continuous effect is still pumped.
 */
val ThistledownLiege = card("Thistledown Liege") {
    manaCost = "{1}{W/U}{W/U}{W/U}"
    typeLine = "Creature — Kithkin Knight"
    power = 1
    toughness = 3
    oracleText = "Flash\n" +
        "Other white creatures you control get +1/+1.\n" +
        "Other blue creatures you control get +1/+1."

    keywords(Keyword.FLASH)

    // Other white creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.WHITE).youControl(),
                excludeSelf = true
            )
        )
    }

    // Other blue creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.BLUE).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Adam Rex"
        flavorText = "The thoughtweft is his informant, and he its devoted guardian."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20e34233-0972-4aa7-a5ab-eabed2235148.jpg?1783942735"
    }
}
