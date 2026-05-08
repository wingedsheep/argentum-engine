package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Nightwhorl Hermit
 * {2}{U}
 * Creature — Rat Rogue
 * 1/4
 *
 * Vigilance
 * Threshold — As long as there are seven or more cards in your graveyard,
 * this creature gets +1/+0 and can't be blocked.
 */
val NightwhorlHermit = card("Nightwhorl Hermit") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Rat Rogue"
    power = 1
    toughness = 4
    oracleText = "Vigilance\nThreshold — As long as there are seven or more cards in your graveyard, this creature gets +1/+0 and can't be blocked."

    keywords(Keyword.VIGILANCE)

    // Threshold: +1/+0
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7)
        )
    }

    // Threshold: can't be blocked
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.CardsInGraveyardAtLeast(7)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Valera Lutfullina"
        flavorText = "\"He etches his secrets onto the shimmering shells of mussels, saving them to share with the Great Snail.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0928e04f-2568-41e8-b603-7a25cf5f94d0.jpg?1721426168"
    }
}
