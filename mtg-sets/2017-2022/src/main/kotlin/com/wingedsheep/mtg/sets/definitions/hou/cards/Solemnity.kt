package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantGetCounters
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Solemnity
 * {2}{W}
 * Enchantment
 * Players can't get counters.
 * Counters can't be put on artifacts, creatures, enchantments, or lands.
 *
 * Two statics: [PlayersCantGetCounters] for every player, and [CantReceiveCounters] over the four
 * named permanent types — a noncreature planeswalker still gets loyalty counters.
 */
val Solemnity = card("Solemnity") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Players can't get counters.\n" +
        "Counters can't be put on artifacts, creatures, enchantments, or lands."

    staticAbility { ability = PlayersCantGetCounters() }
    staticAbility {
        ability = CantReceiveCounters(
            filter = GroupFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Creature or
                    GameObjectFilter.Enchantment or GameObjectFilter.Land
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Greg Opalinski"
        flavorText = "The arrival of the God-Pharaoh marked a betrayal of both gods and mortals."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a71fb62-acbd-49f5-842f-0fc9fa48afea.jpg?1783936059"
    }
}
