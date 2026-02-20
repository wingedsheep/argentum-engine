package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TradeSecretsEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Trade Secrets
 * {1}{U}{U}
 * Sorcery
 * Target opponent draws two cards, then you draw up to four cards.
 * That opponent may repeat this process as many times as they choose.
 */
val TradeSecrets = card("Trade Secrets") {
    manaCost = "{1}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Target opponent draws two cards, then you draw up to four cards. That opponent may repeat this process as many times as they choose."

    spell {
        target = TargetOpponent()
        effect = TradeSecretsEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Ron Spears"
        flavorText = "\"The Cabal loves only secrets.\" —Empress Llawan"
        imageUri = "https://cards.scryfall.io/large/front/e/9/e92e197e-ef7e-46bb-9533-5f9819d545b2.jpg?1562945484"
    }
}
