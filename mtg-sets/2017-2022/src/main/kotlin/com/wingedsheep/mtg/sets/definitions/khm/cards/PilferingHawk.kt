package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val PilferingHawk = card("Pilfering Hawk") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Bird"
    oracleText = "Flying\n{S}, {T}: Draw a card, then discard a card. ({S} can be paid with one mana from a snow source.)"
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{S}"), Costs.Tap)
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
        description = "{S}, {T}: Draw a card, then discard a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Dan Murayama Scott"
        flavorText = "\"Well, at least it left us a mouse in exchange. Er . . . ugh . . . make that half a mouse.\"\n—Binhald, Beskir veteran"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76ee3829-8dec-4c0f-a6c2-ad47a1c94cfc.jpg?1783928258"
    }
}
