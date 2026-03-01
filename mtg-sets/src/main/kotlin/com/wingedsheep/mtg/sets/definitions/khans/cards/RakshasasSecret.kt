package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Rakshasa's Secret
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards. You mill two cards.
 */
val RakshasasSecret = card("Rakshasa's Secret") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. You mill two cards."

    spell {
        val t = target("target opponent", TargetOpponent())
        effect = Effects.Discard(2, t)
            .then(Effects.Mill(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Magali Villeneuve"
        flavorText = "The voice of a rakshasa is soft, its breath sweet. But every word is the murmur of madness."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae62a43e-36ce-471e-93f1-21fe674858b5.jpg?1562791960"
    }
}
