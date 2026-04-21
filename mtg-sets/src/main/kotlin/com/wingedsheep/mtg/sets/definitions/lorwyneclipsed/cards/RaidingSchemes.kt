package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Raiding Schemes
 * {3}{R}{G}
 * Enchantment
 *
 * Each noncreature spell you cast has conspire. (As you cast a noncreature spell, you may
 * tap two untapped creatures you control that share a color with it. When you do, copy it
 * and you may choose new targets for the copy. A copy of a permanent spell becomes a token.)
 */
val RaidingSchemes = card("Raiding Schemes") {
    manaCost = "{3}{R}{G}"
    typeLine = "Enchantment"
    oracleText = "Each noncreature spell you cast has conspire. (As you cast a noncreature " +
        "spell, you may tap two untapped creatures you control that share a color with it. " +
        "When you do, copy it and you may choose new targets for the copy. A copy of a " +
        "permanent spell becomes a token.)"

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONSPIRE,
            spellFilter = GameObjectFilter.Noncreature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Justin Gerard"
        flavorText = "\"Keep enemies close . . . and elves closer.\"\n—A tale of Auntie Wort"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bab5da5-72a7-4340-9b53-492ab14a9f71.jpg?1767952283"
    }
}
