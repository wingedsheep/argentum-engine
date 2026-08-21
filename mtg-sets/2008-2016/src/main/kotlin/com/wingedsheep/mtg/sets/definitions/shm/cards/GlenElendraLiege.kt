package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Glen Elendra Liege
 * {1}{U/B}{U/B}{U/B}
 * Creature — Faerie Knight
 * 2 / 3
 *
 * Flying
 * Other blue creatures you control get +1/+1.
 * Other black creatures you control get +1/+1.
 *
 * - Two deliberately separate [ModifyStats] statics, exactly as on Wilt-Leaf Liege: they stack, so
 *   a creature you control that is both blue *and* black gets +2/+2.
 * - `excludeSelf = true` carries the printed "Other" — the Liege is blue *and* black itself, so
 *   without it it would pump itself twice.
 * - The colour test reads projected state through the [GroupFilter], so a creature that only
 *   becomes blue or black from a continuous effect is still pumped.
 */
val GlenElendraLiege = card("Glen Elendra Liege") {
    manaCost = "{1}{U/B}{U/B}{U/B}"
    typeLine = "Creature — Faerie Knight"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Other blue creatures you control get +1/+1.\n" +
        "Other black creatures you control get +1/+1."

    keywords(Keyword.FLYING)

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

    // Other black creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.BLACK).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Kev Walker"
        flavorText = "Those who displease Oona soon learn the extent of the armies she commands."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94cc72b7-b593-438f-a07a-35f2452e1c48.jpg?1783942732"
    }
}
