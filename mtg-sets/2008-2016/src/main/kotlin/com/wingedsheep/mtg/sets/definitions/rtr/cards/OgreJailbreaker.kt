package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ogre Jailbreaker — Return to Ravnica #72
 * {3}{B} · Creature — Ogre Rogue · 4/4
 *
 * Defender
 * This creature can attack as though it didn't have defender as long as you control a Gate.
 */
val OgreJailbreaker = card("Ogre Jailbreaker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Rogue"
    power = 4
    toughness = 4
    oracleText = "Defender\nThis creature can attack as though it didn't have defender as long as you control a Gate."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Gate"))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Karl Kopinski"
        flavorText = "It took three years for it to occur to him to use the chain as a weapon, " +
            "but once it did, he made up for lost time."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a96c83d-96d9-4f8f-8020-77990130ad81.jpg?1783940361"
    }
}
