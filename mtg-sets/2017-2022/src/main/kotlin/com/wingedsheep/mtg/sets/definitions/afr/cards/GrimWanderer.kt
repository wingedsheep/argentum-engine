package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grim Wanderer
 * {1}{B}
 * Creature — Goblin Warlock
 * 5/3
 * Flash
 * Tragic Backstory — Cast this spell only if a creature died this turn.
 *
 * "A creature died" counts creatures under any player's control (per the 2021-07-23 ruling), so
 * the restriction reads the global [Conditions.CreatureDiedThisTurn], not the controlled variant.
 */
val GrimWanderer = card("Grim Wanderer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Warlock"
    power = 5
    toughness = 3
    oracleText = "Flash\nTragic Backstory — Cast this spell only if a creature died this turn."

    keywords(Keyword.FLASH)

    spell {
        castOnlyIf(Conditions.CreatureDiedThisTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Jason A. Engle"
        flavorText = "There is a stillness that walks with him, the quiet that follows great tragedy, " +
            "when even the mourners are too exhausted to weep."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e339f0f7-a79d-4947-a9c4-b9a0949dd06a.jpg?1783926494"
        ruling("2021-07-23", "Grim Wanderer's ability doesn't care who controlled the creature that died.")
    }
}
