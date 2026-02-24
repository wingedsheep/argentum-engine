package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantShroudToController

/**
 * True Believer
 * {W}{W}
 * Creature — Human Cleric
 * 2/2
 * You have shroud. (You can't be the target of spells or abilities.)
 */
val TrueBeliever = card("True Believer") {
    manaCost = "{W}{W}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "You have shroud. (You can't be the target of spells or abilities.)"

    staticAbility {
        ability = GrantShroudToController
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Kev Walker"
        flavorText = "So great is his devotion that he is immune to any spell save his own."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4289bdcb-6eea-458f-a4eb-89e26264673a.jpg?1562910674"
    }
}
