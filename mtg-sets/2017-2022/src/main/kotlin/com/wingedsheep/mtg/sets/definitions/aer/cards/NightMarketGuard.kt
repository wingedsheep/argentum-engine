package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Night Market Guard
 * {3}
 * Artifact Creature — Construct
 * 3/1
 *
 * This creature can block an additional creature each combat.
 */
val NightMarketGuard = card("Night Market Guard") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 1
    oracleText = "This creature can block an additional creature each combat."

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.source(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Steve Argyle"
        flavorText = "\"One head watches for Consulate enforcers. The other watches for renegade thieves.\"\n—Crime Lord Gonti"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ae03e5c-9655-425c-90ef-b70a0e66868d.jpg?1783936724"
    }
}
