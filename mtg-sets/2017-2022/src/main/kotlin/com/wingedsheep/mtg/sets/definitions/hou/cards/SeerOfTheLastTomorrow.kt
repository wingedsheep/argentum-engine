package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SeerOfTheLastTomorrow = card("Seer of the Last Tomorrow") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Cleric"
    oracleText = "{U}, {T}, Discard a card: Target player mills three cards."
    power = 1
    toughness = 4

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap, Costs.DiscardCard)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Sara Winters"
        flavorText = "\"The truth? It's been here all along. The Hours as we understood them are lies.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e897f14-8775-4bc3-a3b2-b149f79ec702.jpg?1783936049"
    }
}
