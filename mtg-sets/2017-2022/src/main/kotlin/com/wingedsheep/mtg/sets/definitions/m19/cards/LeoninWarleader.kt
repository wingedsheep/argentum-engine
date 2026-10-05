package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Leonin Warleader
 * {2}{W}{W}
 * Creature — Cat Soldier
 * 4/4
 * Whenever this creature attacks, create two 1/1 white Cat creature tokens with lifelink that are
 * tapped and attacking.
 */
val LeoninWarleader = card("Leonin Warleader") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks, create two 1/1 white Cat creature tokens with " +
        "lifelink that are tapped and attacking."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            keywords = setOf(Keyword.LIFELINK),
            tapped = true,
            attacking = true,
            imageUri = "https://cards.scryfall.io/normal/front/b/3/b31f1580-5bba-4cef-b0c8-f2837a597b7d.jpg?1783934470"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Jakub Kasper"
        flavorText = "When one leonin hunts, many more are surely nearby."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b31b2e5e-6572-462a-9fa0-1b2e660099e3.jpg?1783934603"
        ruling(
            "2018-07-13",
            "You choose which players or planeswalkers the two tokens are attacking. They don't have to be " +
                "attacking the same player or planeswalker that Leonin Warleader is attacking, and they can each " +
                "be attacking different players and/or planeswalkers."
        )
        ruling(
            "2018-07-13",
            "Although the tokens are attacking, they were never declared as attacking creatures (for the " +
                "purposes of abilities that trigger whenever a creature attacks, for example)."
        )
    }
}
