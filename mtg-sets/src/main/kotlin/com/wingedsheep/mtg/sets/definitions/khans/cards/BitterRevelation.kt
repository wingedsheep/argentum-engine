package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bitter Revelation
 * {3}{B}
 * Sorcery
 * Look at the top four cards of your library. Put two of them into your hand
 * and the rest into your graveyard. You lose 2 life.
 */
val BitterRevelation = card("Bitter Revelation") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"

    spell {
        effect = EffectPatterns.lookAtTopAndKeep(
            count = 4,
            keepCount = 2
        ) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Viktor Titov"
        flavorText = "\"Here you lie then, Ugin. The corpses of worlds will join you in the tomb.\" —Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e42f5aa9-2a47-47c3-ab87-dc25735b0e54.jpg?1562795020"
    }
}
