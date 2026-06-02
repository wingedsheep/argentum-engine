package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofFromMonocoloredToGroup
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Dragonfire Blade
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+2 and has hexproof from monocolored.
 * Equip {4}. This ability costs {1} less to activate for each color of the creature it targets.
 *
 * The equip cost reduction is modeled with the engine's per-activation generic cost reduction
 * (`ActivatedAbility.genericCostReduction`) fed a dynamic amount that reads the chosen target's
 * color count — `DynamicAmounts.targetColorCount()`. The reduction resolves against the target
 * creature the player picks when activating Equip.
 */
val DragonfireBlade = card("Dragonfire Blade") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has hexproof from monocolored.\n" +
        "Equip {4}. This ability costs {1} less to activate for each color of the creature it targets."

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantHexproofFromMonocoloredToGroup()
    }

    equipAbility("{4}", genericCostReduction = DynamicAmounts.targetColorCount())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Clint Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/031afea3-fbfb-4663-a8cc-9b7eb7b16020.jpg?1743204949"
    }
}
