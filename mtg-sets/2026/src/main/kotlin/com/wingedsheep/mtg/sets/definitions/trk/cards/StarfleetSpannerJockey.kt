package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Starfleet Spanner Jockey
 * {1}{U}
 * Creature — Human Artificer
 * 3/3
 *
 * Defender
 * As long as you've cast a noncreature spell this turn, this creature can attack as though it
 * didn't have defender.
 */
val StarfleetSpannerJockey = card("Starfleet Spanner Jockey") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    oracleText = "Defender\n" +
        "As long as you've cast a noncreature spell this turn, this creature can attack as though " +
        "it didn't have defender."
    power = 3
    toughness = 3

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.Noncreature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Chris Rallis"
        flavorText = "A resourceful crew can solve most any problem with elbow grease and dilithium."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16bc9660-2bef-4926-b0e6-f49b22817bea.jpg?1793361250"
    }
}
