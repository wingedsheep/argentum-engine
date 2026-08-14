package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lake-town Lookout
 * {W}
 * Creature — Human Scout
 * 1/1
 *
 * When this creature dies, recruit.
 *
 * Recruit is a keyword action with fixed reminder text, composed by
 * [Patterns.Mechanic.recruit] — draw, discard, and a 1/1 Human Soldier if the discard was a
 * nonland card. Nothing about it reads the dying creature, so the ordinary SELF-bound
 * [Triggers.Dies] is all the binding this needs.
 */
val LaketownLookout = card("Lake-town Lookout") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    oracleText = "When this creature dies, recruit. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, create a 1/1 white Human Soldier creature token.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Irina Nordsol"
        flavorText = "\"Look! The lights again! Last night the watchmen saw them start and fade " +
            "from midnight until dawn. Something is happening up there.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/178c4cf6-6b11-40e4-9673-c560d6818a6b.jpg?1785496946"
    }
}
