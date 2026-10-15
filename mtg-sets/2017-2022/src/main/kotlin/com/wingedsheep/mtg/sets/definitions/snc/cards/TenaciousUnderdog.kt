package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Tenacious Underdog
 * {1}{B}
 * Creature — Human Warrior
 * 3 / 2
 * Blitz—{2}{B}{B}, Pay 2 life. (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 * You may cast this card from your graveyard using its blitz ability.
 */
val TenaciousUnderdog = card("Tenacious Underdog") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    oracleText = "Blitz—{2}{B}{B}, Pay 2 life. (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)\nYou may cast this card from your graveyard using its blitz ability."
    power = 3
    toughness = 2

    keywordAbility(KeywordAbility.blitz("{2}{B}{B}", Costs.additional.PayLife(2)))

    staticAbility {
        ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.BLITZ)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Zara Alfonso"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b6a6d00-3e00-4827-8420-13343ac3d0fd.jpg?1783923123"
    }
}
