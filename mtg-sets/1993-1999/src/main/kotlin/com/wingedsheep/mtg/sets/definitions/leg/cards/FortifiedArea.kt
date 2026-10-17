package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fortified Area
 * {1}{W}{W}
 * Enchantment
 *
 * Wall creatures you control get +1/+0 and have banding.
 */
val FortifiedArea = card("Fortified Area") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Wall creatures you control get +1/+0 and have banding. (Any creatures with banding, and up to one without, can attack in a band. Bands are blocked as a group. If any creatures with banding you control are blocking or being blocked by a creature, you divide that creature's combat damage, not its controller, among any of the creatures it's being blocked by or is blocking.)"

    val walls = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.WALL).youControl())

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = walls)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.BANDING, walls)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc64f19c-5b2b-4697-b4dc-2be9c3790794.jpg?1783948086"
    }
}
