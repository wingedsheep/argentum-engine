package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Veteran's Powerblade
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +2/+0.
 * Equip Soldier {W}
 * Equip {2}
 */
val VeteransPowerblade = card("Veteran's Powerblade") {
    manaCost = "{3}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Equip Soldier {W}\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    // Equip Soldier {W}: attach only to a Soldier creature you control, sorcery speed (CR 702.6c).
    equipAbility(
        "{W}",
        quality = "Soldier",
        targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.SOLDIER),
    )

    // Equip {2}: attach to any creature you control, sorcery speed.
    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Alexandre Honoré"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d823e1-a7d6-4296-94c8-b8ba9b4daa38.jpg?1783920118"
    }
}
