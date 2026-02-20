package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Cruel Tutor
 * {2}{B}
 * Sorcery
 * Search your library for a card, then shuffle and put that card on top. You lose 2 life.
 */
val CruelTutor = card("Cruel Tutor") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = CompositeEffect(
            listOf(
                Effects.SearchLibrary(
                    filter = GameObjectFilter.Any,
                    destination = SearchDestination.TOP_OF_LIBRARY
                ),
                LoseLifeEffect(2, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Brian Snoddy"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c05bfb5-dd36-44c5-a60d-43f7f8c68a6b.jpg"
    }
}
