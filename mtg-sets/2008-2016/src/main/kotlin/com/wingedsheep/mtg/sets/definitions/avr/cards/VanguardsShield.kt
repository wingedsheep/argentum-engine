package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Vanguard's Shield
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +0/+3 and can block an additional creature each combat.
 * Equip {3}
 */
val VanguardsShield = card("Vanguard's Shield") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+3 and can block an additional creature each combat.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility { ability = ModifyStats(0, 3, Filters.EquippedCreature) }
    staticAbility { ability = CanBlockAdditionalForCreatureGroup(count = 1, filter = Filters.EquippedCreature) }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Ryan Pancoast"
        flavorText = "Wars are not fought on a single front."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce8d9db6-5737-4a1f-ae4e-75821a602784.jpg?1783940649"
        ruling("2012-05-01", "Abilities that allow a creature to block an additional creature are cumulative.")
    }
}
