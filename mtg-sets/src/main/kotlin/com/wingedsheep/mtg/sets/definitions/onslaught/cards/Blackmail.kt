package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BlackmailEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Blackmail
 * {B}
 * Sorcery
 * Target player reveals three cards from their hand and you choose one of them.
 * That player discards that card.
 */
val Blackmail = card("Blackmail") {
    manaCost = "{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetPlayer()
        effect = BlackmailEffect(3, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Christopher Moeller"
        flavorText = "\"Even the most virtuous person is only one secret away from being owned by the Cabal.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0397ace0-83df-48ce-8c63-69c4b4ee4ec8.jpg"
    }
}
