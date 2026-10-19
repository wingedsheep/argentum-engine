package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mountain Stronghold
 * Land
 *
 * Red legendary creatures you control have "bands with other legendary creatures."
 *
 * The grant is the land's only text — it taps for no mana. The granted "bands with other legendary
 * creatures" (CR 702.22b) lets any legendary creatures attack as a band once one of them is a
 * red legendary creature you control, and hands combat-damage division to you when two of your
 * legendary creatures (one of them red) block or are blocked by the same creature.
 */
val MountainStronghold = card("Mountain Stronghold") {
    typeLine = "Land"
    oracleText = "Red legendary creatures you control have \"bands with other legendary creatures.\" " +
        "(Any legendary creatures can attack in a band as long as at least one has \"bands with other " +
        "legendary creatures.\" Bands are blocked as a group. If at least two legendary creatures you " +
        "control, one of which has \"bands with other legendary creatures,\" are blocking or being blocked " +
        "by the same creature, you divide that creature's combat damage, not its controller, among any of " +
        "the creatures it's being blocked by or is blocking.)"

    staticAbility {
        ability = GrantKeyword(
            KeywordAbility.bandsWithOther(Supertype.LEGENDARY),
            GroupFilter(GameObjectFilter.Creature.withColor(Color.RED).legendary().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "304"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/314fd1d7-4bd8-4d95-b7c2-1aa6660ab88a.jpg?1783948022"
    }
}
