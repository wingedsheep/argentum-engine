package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Blade Instructor
 * {2}{W}
 * Creature — Human Soldier
 * 3/1
 *
 * Mentor
 */
val BladeInstructor = card("Blade Instructor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 1
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)"

    mentor()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Zoltan Boros"
        flavorText = "\"Watch carefully. The gap between death and victory is thinner than your blade.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b1c7f07-8d39-425b-8ae9-b3ab317cc0fe.jpg"
    }
}
