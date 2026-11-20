package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scholar of Athreos
 * {2}{W}
 * Creature — Human Cleric
 * 1/4
 * {2}{B}: Each opponent loses 1 life. You gain life equal to the life lost this way.
 *
 * [Effects.DrainLife] is the "each opponent loses X, you gain the life lost this way" shape.
 */
val ScholarOfAthreos = card("Scholar of Athreos") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "{2}{B}: Each opponent loses 1 life. You gain life equal to the life lost this way."

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.DrainLife(1)
        description = "{2}{B}: Each opponent loses 1 life. You gain life equal to the life lost this way."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Cynthia Sheppard"
        flavorText = "She asks pointed questions of the dead who wait for Athreos, learning of life from those who are about to leave it."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a36e06-39f3-47d0-b6a9-43728afceb4a.jpg?1783939808"
    }
}
