package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cultist's Staff (Eldritch Moon #194)
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+2.
 * Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)
 */
val CultistsStaff = card("Cultist's Staff") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Adam Paquette"
        flavorText = "The head of the staff doubles as a brand to anoint the newly inducted."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8da8c523-44e8-43e7-8431-28dbed1015ac.jpg"
    }
}
