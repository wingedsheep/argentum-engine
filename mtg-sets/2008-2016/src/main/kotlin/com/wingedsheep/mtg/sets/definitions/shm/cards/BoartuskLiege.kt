package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Boartusk Liege
 * {1}{R/G}{R/G}{R/G}
 * Creature — Goblin Knight
 * 3 / 4
 *
 * Trample
 * Other red creatures you control get +1/+1.
 * Other green creatures you control get +1/+1.
 *
 * - Two deliberately separate [ModifyStats] statics, exactly as on Wilt-Leaf Liege: they stack, so
 *   a creature you control that is both red *and* green gets +2/+2.
 * - `excludeSelf = true` carries the printed "Other" — the Liege is red *and* green itself, so
 *   without it it would pump itself twice.
 * - The colour test reads projected state through the [GroupFilter], so a creature that only
 *   becomes red or green from a continuous effect is still pumped.
 */
val BoartuskLiege = card("Boartusk Liege") {
    manaCost = "{1}{R/G}{R/G}{R/G}"
    typeLine = "Creature — Goblin Knight"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "Other red creatures you control get +1/+1.\n" +
        "Other green creatures you control get +1/+1."

    keywords(Keyword.TRAMPLE)

    // Other red creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.RED).youControl(),
                excludeSelf = true
            )
        )
    }

    // Other green creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.GREEN).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Jesper Ejsing"
        flavorText = "The boar leads its rider to victory in battle, but it doesn't know how close it is to becoming the victory feast."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/318338f8-f52b-4b9a-8d38-08291bcc2e98.jpg?1783942723"
    }
}
