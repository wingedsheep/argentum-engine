package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thundercloud Elemental
 * {5}{U}{U}
 * Creature — Elemental
 * 3/4
 * Flying
 * {3}{U}: Tap all creatures with toughness 2 or less.
 * {3}{U}: All other creatures lose flying until end of turn.
 */
val ThundercloudElemental = card("Thundercloud Elemental") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "Flying\n{3}{U}: Tap all creatures with toughness 2 or less.\n{3}{U}: All other creatures lose flying until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.TapAll(Filters.Group.creatures { toughnessAtMost(2) })
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.RemoveKeywordFromAll(Keyword.FLYING, Filters.Group.otherCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Anthony S. Waters"
        flavorText = "Some days it's better to stay inside."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/597aea42-43e0-41ed-bfe7-fc92b6b8e680.jpg?1562529487"
    }
}
