package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Hammer Dropper
 * {2}{R}{W}
 * Creature — Giant Soldier
 * 5/2
 *
 * Mentor
 */
val HammerDropper = card("Hammer Dropper") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Giant Soldier"
    power = 5
    toughness = 2
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)"

    mentor()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Lake Hurwitz"
        flavorText = "Giants know a solid hit is one part strength, four parts leverage."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9efac789-587b-45c1-b16e-616e9486b59b.jpg"
    }
}
