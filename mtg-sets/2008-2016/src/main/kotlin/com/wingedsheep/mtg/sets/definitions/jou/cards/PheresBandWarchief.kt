package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pheres-Band Warchief
 * {3}{G}
 * Creature — Centaur Warrior
 * 3/3
 *
 * Vigilance, trample
 * Other Centaur creatures you control get +1/+1 and have vigilance and trample.
 *
 * The Warchief's own vigilance and trample are printed; the three lord statics share one
 * "other Centaur creatures you control" group (`excludeSelf` is "Other").
 */
val PheresBandWarchief = card("Pheres-Band Warchief") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 3
    toughness = 3
    oracleText = "Vigilance, trample\nOther Centaur creatures you control get +1/+1 and have vigilance and trample."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    val otherCentaurs = GroupFilter(
        GameObjectFilter.Creature.withSubtype(Subtype.CENTAUR).youControl(),
        excludeSelf = true,
    )

    staticAbility {
        ability = ModifyStats(1, 1, otherCentaurs)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, otherCentaurs)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, otherCentaurs)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "James Ryman"
        flavorText = "The renowned warchiefs of Pheres Band fight with the might of minotaurs and the " +
            "sagacity of sphinxes."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8456730a-607c-44c0-b18e-0df8f35b0634.jpg?1783939409"
    }
}
