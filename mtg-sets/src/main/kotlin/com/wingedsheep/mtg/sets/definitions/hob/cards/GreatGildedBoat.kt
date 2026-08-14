package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Great Gilded Boat
 * {2}{U}
 * Artifact — Vehicle
 * 4/4
 *
 * Whenever you attack, recruit.
 * Crew 2
 *
 * "Whenever you attack" is the once-per-combat declare-attackers trigger ([Triggers.YouAttack]),
 * not a per-attacker one — it fires once however many creatures were declared, and it fires even
 * when the Boat itself is uncrewed and stays home.
 */
val GreatGildedBoat = card("Great Gilded Boat") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    oracleText = "Whenever you attack, recruit. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, create a 1/1 white Human Soldier creature token.)\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Patterns.Mechanic.recruit()
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2fb3995-5b43-4776-88b2-346d353edee0.jpg?1784862975"
    }
}
