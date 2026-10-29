package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Fallaji Wayfarer
 * {2}{G}
 * Creature — Human Scout
 * 2/4
 *
 * Fallaji Wayfarer is all colors. This ability doesn't affect its color identity.
 * Multicolored spells you cast have convoke.
 *
 * "Is all colors" is a characteristic-defining ability (CR 604.3), so it holds in every zone and
 * applies before any other color-changing effect — the same footing as a WUBRG color indicator,
 * which is how it is modelled. The color identity stays green, as the card says. Convoke is
 * granted to spells whose colors on the stack number two or more.
 */
val FallajiWayfarer = card("Fallaji Wayfarer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    colorIndicator = "WUBRG"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 4
    oracleText = "Fallaji Wayfarer is all colors. This ability doesn't affect its color identity. " +
        "(It can be in any deck whose commander's color identity includes green.)\n" +
        "Multicolored spells you cast have convoke. (Your creatures can help cast those spells. " +
        "Each creature you tap while casting a multicolored spell pays for {1} or one mana of a " +
        "color that creature is.)"

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Multicolored,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc2378b2-96e2-44ca-8434-9f06c26b183d.jpg?1783921474"
    }
}
