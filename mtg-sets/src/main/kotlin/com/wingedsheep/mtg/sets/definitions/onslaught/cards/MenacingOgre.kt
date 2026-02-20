package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.triggers.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.effects.SecretBidEffect

/**
 * Menacing Ogre
 * {3}{R}{R}
 * Creature — Ogre
 * 3/3
 * Trample, haste
 * When Menacing Ogre enters the battlefield, each player secretly chooses a number.
 * Then those numbers are revealed. Each player with the highest number loses that much
 * life. If you are one of those players, put two +1/+1 counters on Menacing Ogre.
 */
val MenacingOgre = card("Menacing Ogre") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Ogre"
    power = 3
    toughness = 3
    oracleText = "Trample, haste\nWhen Menacing Ogre enters, each player secretly chooses a number. Then those numbers are revealed. Each player with the highest number loses that much life. If you are one of those players, put two +1/+1 counters on Menacing Ogre."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = SecretBidEffect(
            counterType = "+1/+1",
            counterCount = 2
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Pete Venters"
        flavorText = "The Skirk Ridge goblins had never seen an ogre before. They would never see one again."
        imageUri = "https://cards.scryfall.io/large/front/5/3/5360a871-6932-45b2-bc94-1bd414e38906.jpg?1562914555"
    }
}
