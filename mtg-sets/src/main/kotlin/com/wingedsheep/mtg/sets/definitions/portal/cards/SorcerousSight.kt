package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtHandEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Sorcerous Sight
 * {U}
 * Sorcery
 * Look at target opponent's hand. Draw a card.
 */
val SorcerousSight = card("Sorcerous Sight") {
    manaCost = "{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = CompositeEffect(
            listOf(
                LookAtHandEffect(EffectTarget.ContextTarget(0)),
                DrawCardsEffect(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9424e0e9-4fda-4b5b-9dd0-f3c1d7fb2a08.jpg"
    }
}
