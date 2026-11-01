package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Gruul Spellbreaker — Ravnica Allegiance #179
 * {1}{R}{G} · Creature — Ogre Warrior · 3/3
 *
 * Riot, trample. "During your turn, you and this creature have hexproof" is two statics behind
 * the same [Conditions.IsYourTurn] gate: [GrantHexproofToController] for the player and
 * [GrantKeyword] for the creature itself.
 */
val GruulSpellbreaker = card("Gruul Spellbreaker") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "Trample\n" +
        "During your turn, you and this creature have hexproof."

    riot()
    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantHexproofToController,
            condition = Conditions.IsYourTurn,
        )
    }

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Zoltan Boros"
        flavorText = "She displays her scars with pride."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/326679a2-782d-45a0-9a06-b147ceff3979.jpg"
    }
}
