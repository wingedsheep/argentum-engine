package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Team Pennant — Strixhaven: School of Mages #260 (canonical printing)
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has vigilance and trample.
 * Equip creature token {1}
 * Equip {3}
 *
 * Kor Halberd's static shape plus two equip abilities: the cheap one is an "Equip [quality]"
 * variant (CR 702.6c) restricted to creature tokens you control, Blackblade Reforged's pairing.
 */
val TeamPennant = card("Team Pennant") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has vigilance and trample.\n" +
        "Equip creature token {1}\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    equipAbility(
        "{1}",
        quality = "creature token",
        targetFilter = TargetFilter(GameObjectFilter.Creature.token().youControl()),
    )

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Anna Fehr"
        flavorText = "Choosing a college based on Mage Tower results is discouraged, but not unheard of."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be294c4e-712b-459c-a36c-e26ed5c27edb.jpg?1783927279"
    }
}
