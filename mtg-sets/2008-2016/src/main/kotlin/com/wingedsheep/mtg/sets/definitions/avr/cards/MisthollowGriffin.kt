package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Misthollow Griffin
 * {2}{U}{U}
 * Creature — Griffin
 * 3/3
 * Flying
 * You may cast this card from exile.
 */
val MisthollowGriffin = card("Misthollow Griffin") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Griffin"
    power = 3
    toughness = 3
    oracleText = "Flying\nYou may cast this card from exile."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MayCastSelfFromZones(zones = listOf(Zone.EXILE))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Jaime Jones"
        flavorText = "For some creatures, existing is only a whim."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2db4fe28-e580-479b-910f-b719d69468b1.jpg?1783940715"
        ruling("2012-05-01", "Misthollow Griffin can also be cast from a player's hand.")
        ruling("2012-05-01", "Misthollow Griffin doesn't have an ability that exiles itself. Some other effect will be needed to get the card into exile.")
        ruling("2012-05-01", "You must still pay Misthollow Griffin's mana cost when you cast it from exile.")
        ruling("2012-05-01", "Misthollow Griffin's ability doesn't change when you can cast creature spells: when you have priority during your main phase when the stack is empty.")
    }
}
