package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Predefined token CardDefinitions.
 *
 * These are registered in the CardRegistry so that the engine can look up token abilities
 * by name (e.g., "Treasure" → its mana ability). The unified [CreatePredefinedTokenExecutor]
 * creates entities with a matching `name` field, and the engine resolves abilities via
 * `cardRegistry.getCard(name)`.
 *
 * To add a new predefined token type, define it here and add a facade method to `Effects.kt`.
 */
object PredefinedTokens {

    /**
     * Treasure token — an artifact with:
     * "{T}, Sacrifice this artifact: Add one mana of any color."
     */
    val Treasure = card("Treasure") {
        typeLine = "Artifact - Treasure"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/4/8/4837a3f1-ca7f-41e5-a5d1-729c8495b0e8.jpg?1771590279"
        }
    }

    /**
     * Food token — an artifact with:
     * "{2}, {T}, Sacrifice this artifact: You gain 3 life."
     */
    val Food = card("Food") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0dce2241-e58b-41d4-b57c-9794fc8ee004.jpg?1721425221"
        }
    }

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
     * "Just One Glass" — a named Food token created by Sekshaas, Early Sleeper.
     * Functionally identical to a Food token, but with custom name and art.
     */
    val JustOneGlass = card("Just One Glass") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "/images/custom/just-one-glass.jpeg"
        }
    }

    /**
     * All predefined token definitions.
     * Register these in the CardRegistry so token abilities are resolved.
     */
    val allTokens: List<CardDefinition> = listOf(
        Treasure,
        Food,
        Lander,
        JustOneGlass
    )
}
