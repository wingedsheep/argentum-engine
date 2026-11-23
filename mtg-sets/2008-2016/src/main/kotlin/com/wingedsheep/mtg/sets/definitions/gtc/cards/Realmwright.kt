package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Realmwright
 * {U}
 * Creature — Vedalken Wizard
 * 1/1
 * As this creature enters, choose a basic land type.
 * Lands you control are the chosen type in addition to their other types.
 *
 * [GrantChosenSubtype] reads the basic-land-type choice (as Thran Portal does); the added type
 * carries its intrinsic mana ability (CR 305.6).
 */
val Realmwright = card("Realmwright") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Wizard"
    power = 1
    toughness = 1
    oracleText = "As this creature enters, choose a basic land type.\n" +
        "Lands you control are the chosen type in addition to their other types."

    replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))

    staticAbility {
        ability = GrantChosenSubtype(filter = GroupFilter.AllLands.youControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/989c76b2-d130-443d-8534-6525fef404c2.jpg?1783940135"
        ruling("2013-01-24", "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Other land types, like Gate, aren't basic lands types.")
        ruling("2013-01-24", "Lands you control will have the ability to tap for the color of mana corresponding to the chosen type. They'll also retain any other abilities they have.")
    }
}
