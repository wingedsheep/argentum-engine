package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Lord Magnus
 * {3}{G}{W}{W}
 * Legendary Creature — Human Druid
 * 4/3
 *
 * First strike
 * Creatures with plainswalk can be blocked as though they didn't have plainswalk.
 * Creatures with forestwalk can be blocked as though they didn't have forestwalk.
 */
val LordMagnus = card("Lord Magnus") {
    manaCost = "{3}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Druid"
    power = 4
    toughness = 3
    oracleText = "First strike\n" +
        "Creatures with plainswalk can be blocked as though they didn't have plainswalk.\n" +
        "Creatures with forestwalk can be blocked as though they didn't have forestwalk."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.PLAINSWALK)
    }

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.FORESTWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a02aabb-c464-4672-b37b-d5d713ef8939.jpg?1783948036"
        ruling(
            "2004-10-04",
            "The statement that creatures with Plainswalk or Forestwalk can be blocked applies to all " +
                "creatures on the battlefield blocking them, and not just to Lord Magnus."
        )
    }
}
