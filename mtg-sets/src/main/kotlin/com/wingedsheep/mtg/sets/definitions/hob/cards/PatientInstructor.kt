package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Patient Instructor
 * {2}{W/U}
 * Creature — Human Citizen
 * 2/2
 *
 * Vigilance
 * When this creature enters, recruit.
 *
 * The hybrid {W/U} makes the card both white and blue, so the colour identity is "WU".
 */
val PatientInstructor = card("Patient Instructor") {
    manaCost = "{2}{W/U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Citizen"
    oracleText = "Vigilance\n" +
        "When this creature enters, recruit. (Draw a card, then discard a card. If you discarded " +
        "a nonland card, create a 1/1 white Human Soldier creature token.)"
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Joshua Raphael"
        flavorText = "Lake-town was much diminished from its old grandeur, but still the Long Lake " +
            "provided for all their needs."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4800508-8bb9-41bb-8712-b55fba7a80a5.jpg?1785323310"
    }
}
