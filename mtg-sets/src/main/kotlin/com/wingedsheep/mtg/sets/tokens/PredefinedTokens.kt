package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Predefined token CardDefinitions.
 *
 * These are registered in the CardRegistry so that the engine can look up token abilities
 * by name (e.g., "Lander" → its activated ability). Token executors create entities with
 * a matching `name` field, and the engine resolves abilities via `cardRegistry.getCard(name)`.
 */
object PredefinedTokens {

    /**
     * Lander token — an artifact with:
     * "{2}, {T}, Sacrifice this token: Search your library for a basic land card,
     * put it onto the battlefield tapped, then shuffle."
     */
    val Lander = card("Lander") {
        typeLine = "Artifact - Lander"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/8/5/85ef1950-219f-401b-8ff5-914f9aaec122.jpg?1752946491"
            artist = "Jorge Jacinto"
            collectorNumber = "8"
        }
    }

    /**
     * All predefined token definitions.
     * Register these in the CardRegistry so token abilities are resolved.
     */
    val allTokens: List<CardDefinition> = listOf(
        Lander
    )
}
