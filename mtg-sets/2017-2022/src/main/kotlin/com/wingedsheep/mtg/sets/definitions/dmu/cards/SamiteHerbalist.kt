package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SamiteHerbalist = card("Samite Herbalist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Whenever this creature becomes tapped, you gain 1 life and scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.GainLife(1) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Alessandra Pisano"
        flavorText = "\"Spiritual wholeness is not something you can give to yourself: it is attained only through the divine blessing of healing another.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/409189da-53c9-4daf-b6b2-d155fc14f91a.jpg?1783921361"
    }
}
