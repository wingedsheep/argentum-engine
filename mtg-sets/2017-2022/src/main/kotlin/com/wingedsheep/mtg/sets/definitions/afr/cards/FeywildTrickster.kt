package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Feywild Trickster
 * {2}{U}
 * Creature — Gnome Warlock
 * 2/2
 * Whenever you roll one or more dice, create a 1/1 blue Faerie Dragon creature token with flying.
 *
 * One token per roll instruction, however many dice it rolled ([Triggers] `rollsDice()`).
 */
val FeywildTrickster = card("Feywild Trickster") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Gnome Warlock"
    oracleText = "Whenever you roll one or more dice, create a 1/1 blue Faerie Dragon creature token with flying."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.rollsDice()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Faerie", "Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/7/8/78eb6e0c-3630-46bf-b124-1ff2ddddf63e.jpg?1783926346",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Iris Compiet"
        flavorText = "Unlike most warlocks, those who make pacts with archfey are drawn to pranks and whimsy moreso than death and darkness."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c93e0296-69e0-45e0-a1c2-0214a53c52d0.jpg?1783926514"
    }
}
