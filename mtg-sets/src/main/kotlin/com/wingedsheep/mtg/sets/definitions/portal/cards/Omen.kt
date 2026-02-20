package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect

/**
 * Omen
 * {1}{U}
 * Sorcery
 * Look at the top three cards of your library, then put them back in any order.
 * You may shuffle. Draw a card.
 */
val Omen = card("Omen") {
    manaCost = "{1}{U}"
    typeLine = "Sorcery"

    spell {
        effect = CompositeEffect(
            listOf(
                EffectPatterns.lookAtTopAndReorder(3),
                MayEffect(ShuffleLibraryEffect()),
                DrawCardsEffect(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6be1956e-c43e-4a6c-950a-5be6e6ca013f.jpg"
    }
}
