package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Cruel Fate
 * {4}{U}
 * Sorcery
 * Look at the top five cards of target opponent's library. Put one of them into that
 * player's graveyard and the rest on top of their library in any order.
 */
val CruelFate = card("Cruel Fate") {
    manaCost = "{4}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = EffectPatterns.lookAtTargetLibraryAndDiscard(
            count = 5,
            toGraveyard = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44bea0d4-946e-4cb8-b6f1-50231d52bfbe.jpg"
    }
}
