package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Ponyback Brigade
 * {3}{R}{W}{B}
 * Creature — Goblin Warrior
 * 2/2
 * When Ponyback Brigade enters or is turned face up, create three 1/1 red Goblin creature tokens.
 * Morph {2}{R}{W}{B}
 */
val PonybackBrigade = card("Ponyback Brigade") {
    manaCost = "{3}{R}{W}{B}"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "When Ponyback Brigade enters or is turned face up, create three 1/1 red Goblin creature tokens.\nMorph {2}{R}{W}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    val tokenEffect = CreateTokenEffect(
        count = 3,
        power = 1,
        toughness = 1,
        colors = setOf(Color.RED),
        creatureTypes = setOf("Goblin"),
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed418a8b-f158-492d-a323-6265b3175292.jpg?1562640121"
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = tokenEffect
    }

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = tokenEffect
    }

    morph = "{2}{R}{W}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/192d77ef-e8b5-44e2-842b-2c1f342c1e69.jpg?1562783191"
    }
}
