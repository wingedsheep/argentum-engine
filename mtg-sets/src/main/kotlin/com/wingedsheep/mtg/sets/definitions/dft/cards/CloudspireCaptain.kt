package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cloudspire Captain — Aetherdrift #9
 * {2}{W} · Creature — Human Pilot · 2/3
 */
val CloudspireCaptain = card("Cloudspire Captain") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Pilot"
    oracleText = "Mounts and Vehicles you control get +1/+1.\n" +
        "This creature saddles Mounts and crews Vehicles as though its power were 2 greater."
    power = 2
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Any.withAnySubtype("Mount", "Vehicle").youControl()
            )
        )
    }

    staticAbility {
        ability = CrewSaddleContribution(modifier = 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Manny Edeko"
        flavorText = "Cloudspire's might comes from its unparalleled coordination."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3380d87a-c460-409c-8d47-9b2fc5ddd2ea.jpg?1783907920"
    }
}
