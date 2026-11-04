package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RiverHoopoe = card("River Hoopoe") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Bird"
    oracleText = "Flying\n{3}{G}{U}: You gain 2 life and draw a card."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}{G}{U}")
        effect = Effects.GainLife(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Ryan Pancoast"
        flavorText = "\"It's a cruel thing to give such a sad song to such a colorful creature.\"\n—Sana, former Ahn-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/585ec4c2-aae2-403e-af32-3324122c472a.jpg?1783936009"
    }
}
