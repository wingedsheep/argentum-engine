package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Parhelion Patrol
 * {3}{W}
 * Creature — Human Knight
 * 2/3
 *
 * Flying
 * Vigilance
 * Mentor
 */
val ParhelionPatrol = card("Parhelion Patrol") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    mentor()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eef126f2-7e72-4338-8021-3ad95e4ab982.jpg?1783934195"
    }
}
