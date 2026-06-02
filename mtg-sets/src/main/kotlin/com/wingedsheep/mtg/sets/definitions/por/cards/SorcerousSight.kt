package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Sorcerous Sight
 * {U}
 * Sorcery
 * Look at target opponent's hand. Draw a card.
 */
val SorcerousSight = card("Sorcerous Sight") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = Effects.Composite(
            listOf(
                LookAtTargetHandEffect(EffectTarget.ContextTarget(0)),
                Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecfd43dc-e5fd-43bc-babb-fe7ecb6ccd00.jpg"
    }
}
