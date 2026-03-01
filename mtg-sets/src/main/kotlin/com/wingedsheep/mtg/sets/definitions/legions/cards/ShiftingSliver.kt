package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeBlockedExceptBySubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Shifting Sliver
 * {3}{U}
 * Creature — Sliver
 * 2/2
 * Slivers can't be blocked except by Slivers.
 */
val ShiftingSliver = card("Shifting Sliver") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Sliver"
    power = 2
    toughness = 2
    oracleText = "Slivers can't be blocked except by Slivers."

    staticAbility {
        ability = GrantCantBeBlockedExceptBySubtype(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver")),
            requiredSubtype = "Sliver"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Darrell Riche"
        flavorText = "Once the last few slivers the Riptide Project controlled were dead, there was nothing to keep the island from being completely overrun."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f68c4c2-91b5-4ffe-9dff-a6834038aa94.jpg?1562901214"
    }
}
