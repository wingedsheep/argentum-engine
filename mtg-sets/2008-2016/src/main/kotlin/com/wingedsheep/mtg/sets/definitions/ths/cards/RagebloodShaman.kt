package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rageblood Shaman
 * {1}{R}{R}
 * Creature — Minotaur Shaman
 * 2/3
 *
 * Trample
 * Other Minotaur creatures you control get +1/+1 and have trample.
 */
val RagebloodShaman = card("Rageblood Shaman") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Shaman"
    power = 2
    toughness = 3
    oracleText = "Trample\nOther Minotaur creatures you control get +1/+1 and have trample."

    keywords(Keyword.TRAMPLE)

    val otherMinotaurs = GroupFilter(
        GameObjectFilter.Creature.withSubtype(Subtype.MINOTAUR).youControl(),
        excludeSelf = true,
    )

    staticAbility {
        ability = ModifyStats(1, 1, otherMinotaurs)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, otherMinotaurs)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Mike Bierek"
        flavorText = "\"I see a spark of pure rage. Soon that spark will spread from the depths of Kragma. " +
            "Soon its fire will engulf the polis.\"\n—Hira, street oracle"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffaa31fd-5ba5-49d4-90b6-fafb9f1a8b3a.jpg?1783939756"
    }
}
