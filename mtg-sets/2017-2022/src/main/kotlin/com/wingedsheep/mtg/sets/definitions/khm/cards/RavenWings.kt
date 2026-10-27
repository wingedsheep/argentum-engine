package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Raven Wings
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+0, has flying, and is a Bird in addition to its other types.
 * Equip {2}
 */
val RavenWings = card("Raven Wings") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0, has flying, and is a Bird in addition to its other types.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        ability = GrantSubtype(Subtype.BIRD.value, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Andrew Mar"
        flavorText = "\"There are some places even our ships cannot travel. I would know their secrets too.\"\n—Inga Rune-Eyes"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60916ebe-6d84-4873-bd91-351bbe219c57.jpg?1783928183"
    }
}
