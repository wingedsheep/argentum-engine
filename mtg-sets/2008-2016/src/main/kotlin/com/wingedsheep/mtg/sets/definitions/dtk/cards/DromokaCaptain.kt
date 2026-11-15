package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dromoka Captain
 * {2}{W}
 * Creature — Human Soldier
 * 1/1
 *
 * First strike
 * Whenever this creature attacks, bolster 1. (Choose a creature with the least toughness
 * among creatures you control and put a +1/+1 counter on it.)
 */
val DromokaCaptain = card("Dromoka Captain") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "First strike\n" +
        "Whenever this creature attacks, bolster 1. (Choose a creature with the least toughness " +
        "among creatures you control and put a +1/+1 counter on it.)"

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d11837c-613f-4e36-b5dd-99baf8b38532.jpg?1783938619"
    }
}
