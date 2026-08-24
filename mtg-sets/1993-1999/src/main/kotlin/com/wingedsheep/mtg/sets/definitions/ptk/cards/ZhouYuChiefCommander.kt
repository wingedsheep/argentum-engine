package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless

/**
 * Zhou Yu, Chief Commander
 * {5}{U}{U}
 * Legendary Creature — Human Soldier
 * 8/8
 *
 * Zhou Yu can't attack unless defending player controls an Island.
 */
val ZhouYuChiefCommander = card("Zhou Yu, Chief Commander") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Soldier"
    power = 8
    toughness = 8
    oracleText = "Zhou Yu can't attack unless defending player controls an Island."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Xu Xiaoming"
        flavorText = "\"After making me, Zhou Yu, did you have to make Kongming?\"\n—Zhou Yu crying to heaven on his deathbed"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e2cf83b-417d-41ca-8e65-86aa65180c40.jpg"
    }
}
