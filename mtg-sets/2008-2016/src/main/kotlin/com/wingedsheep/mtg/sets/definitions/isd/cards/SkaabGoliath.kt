package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skaab Goliath
 * {5}{U}
 * Creature — Zombie Giant
 * 6/9
 * As an additional cost to cast this spell, exile two creature cards from your graveyard.
 * Trample
 */
val SkaabGoliath = card("Skaab Goliath") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Giant"
    power = 6
    toughness = 9
    oracleText = "As an additional cost to cast this spell, exile two creature cards from your graveyard.\nTrample"

    keywords(Keyword.TRAMPLE)

    additionalCost(Costs.additional.ExileCards(count = 2, filter = GameObjectFilter.Creature))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Volkan Baǵa"
        flavorText = "\"Three heads, six arms, and some armor grafts are better than . . . the normal numbers of those things.\"\n—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c1134a5-5434-4733-812b-3587b1817813.jpg?1783940966"
    }
}
