package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Vibranium Energy Daggers
 * {1}
 * Artifact — Equipment
 *
 * Indestructible
 * Equipped creature gets +2/+2.
 * Equip {3}
 *
 * Indestructible is on the Equipment itself (not granted to the equipped creature), so it's a plain
 * keyword; the buff is a no-filter [ModifyStats] which defaults to the attached creature.
 */
val VibraniumEnergyDaggers = card("Vibranium Energy Daggers") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Indestructible (Effects that say \"destroy\" don't destroy this Equipment.)\n" +
        "Equipped creature gets +2/+2.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Irvin Rodriguez"
        flavorText = "\"These daggers can penetrate adamantium and disrupt nervous systems. " +
            "What chance have you?\"\n—T'Challa, the Black Panther"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fe5952d-f50f-443a-b960-657fc4bc1965.jpg?1783902888"
    }
}
