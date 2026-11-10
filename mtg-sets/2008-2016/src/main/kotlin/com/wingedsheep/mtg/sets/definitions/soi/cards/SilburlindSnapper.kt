package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Silburlind Snapper
 * {5}{U}
 * Creature — Turtle
 * 6/6
 * This creature can't attack unless you've cast a noncreature spell this turn.
 *
 * The attack restriction reads the turn's cast history, so a noncreature spell that was countered
 * still unlocks the attack (2016-04-08 ruling).
 */
val SilburlindSnapper = card("Silburlind Snapper") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Turtle"
    oracleText = "This creature can't attack unless you've cast a noncreature spell this turn."
    power = 6
    toughness = 6

    staticAbility {
        ability = CantAttackUnless(
            Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.Noncreature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Tomasz Jedruszek"
        flavorText = "Nephalia is home to many hard-shelled creatures of unusual size, the largest of which are either avoided or worshipped."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7347f4c-a4b3-4c71-91e5-699475e5926e.jpg?1783937787"
        ruling("2016-04-08", "It doesn't matter whether the noncreature spell resolved or was countered, as long as it was cast.")
    }
}
