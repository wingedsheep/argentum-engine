package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Hexgold Halberd
 * {1}{R}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * During your turn, equipped creature has first strike and trample.
 * Equip {2}{R}
 */
val HexgoldHalberd = card("Hexgold Halberd") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "During your turn, equipped creature has first strike and trample.\n" +
        "Equip {2}{R}"

    forMirrodin()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature),
            condition = Conditions.IsYourTurn
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature),
            condition = Conditions.IsYourTurn
        )
    }

    equipAbility("{2}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f22108ec-28f0-44d3-ba6b-3075f5f6bc65.jpg?1783918029"
    }
}
