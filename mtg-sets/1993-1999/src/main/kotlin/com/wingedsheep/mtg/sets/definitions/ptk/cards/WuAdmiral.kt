package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wu Admiral
 * {4}{U}
 * Creature — Human Soldier
 * 3/3
 * This creature gets +1/+1 as long as an opponent controls an Island.
 */
val WuAdmiral = card("Wu Admiral") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "This creature gets +1/+1 as long as an opponent controls an Island."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(
                Player.EachOpponent,
                Zone.BATTLEFIELD,
                GameObjectFilter.Land.withSubtype(Subtype.ISLAND)
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Zhang Jiazhen"
        flavorText = "The Wu kingdom's well-trained admirals were integral to the Southlands' victory at Red Cliffs as well as the kingdom's defense."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/499ae191-80ac-42bf-b49d-db343803bd56.jpg"
    }
}
