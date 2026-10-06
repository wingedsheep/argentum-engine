package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Two-Headed Giant of Foriys
 * {4}{R}
 * Creature — Giant
 * 4/4
 * Trample
 * This creature can block an additional creature each combat.
 *
 * Same shape as Selesnya Sagittars: [CanBlockAdditionalForCreatureGroup] scoped to
 * [GroupFilter.source].
 */
val TwoHeadedGiantOfForiys = card("Two-Headed Giant of Foriys") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "This creature can block an additional creature each combat."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.source(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Anson Maddocks"
        flavorText = "None know if this Giant is the result of aberrant magics, Siamese twins, or a mentalist's schizophrenia."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31c687dc-ee0c-4e54-a2b3-5d8e633b3245.jpg?1783948680"
    }
}
