package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Valley Dasher
 * {1}{R}
 * Creature — Human Berserker
 * 2/2
 * Haste
 * Valley Dasher attacks each combat if able.
 */
val ValleyDasher = card("Valley Dasher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 2
    oracleText = "Haste\nValley Dasher attacks each combat if able."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Matt Stewart"
        flavorText = "\"Mardu riders' greatest fear is that a battle might end before their weapons draw blood.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/8543adbd-0dd1-47d3-ac41-2ec72d6a5d35.jpg?1562789625"
    }
}
