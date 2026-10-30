package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nightsquad Commando — Ikoria: Lair of Behemoths #98
 * {2}{B} · Creature — Human Soldier · 2/3
 *
 * When this creature enters, if you attacked this turn, create a 1/1 white Human Soldier
 * creature token.
 *
 * The "if you attacked this turn" clause is an intervening-if ([Conditions.YouAttackedThisTurn]),
 * checked when the trigger would be put on the stack and again on resolution.
 */
val NightsquadCommando = card("Nightsquad Commando") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, if you attacked this turn, create a 1/1 white Human Soldier creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/90de6d1e-e654-4f4a-8014-061ce69e540f.jpg?1783930948",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Evyn Fong"
        flavorText = "\"The big fella sounds hungry. Let's send him to bed without his supper.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80306d10-0da7-4ee7-b975-ef6c9b535a2a.jpg"
    }
}
