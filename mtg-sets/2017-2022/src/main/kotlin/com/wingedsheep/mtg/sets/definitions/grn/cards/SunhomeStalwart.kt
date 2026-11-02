package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunhome Stalwart
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 *
 * First strike
 * Mentor
 */
val SunhomeStalwart = card("Sunhome Stalwart") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)"

    keywords(Keyword.FIRST_STRIKE)
    mentor()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a431f183-17eb-4a6c-a137-d106bdc8c997.jpg?1783934194"
    }
}
