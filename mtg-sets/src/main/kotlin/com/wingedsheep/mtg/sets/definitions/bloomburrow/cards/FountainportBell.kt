package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Fountainport Bell
 * {1}
 * Artifact
 *
 * When this artifact enters, you may search your library for a basic land card,
 * reveal it, then shuffle and put that card on top.
 * {1}, Sacrifice this artifact: Draw a card.
 */
val FountainportBell = card("Fountainport Bell") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you may search your library for a basic land card, " +
        "reveal it, then shuffle and put that card on top.\n" +
        "{1}, Sacrifice this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                reveal = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeSelf
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "245"
        artist = "Néstor Ossandón Leal"
        flavorText = "Fountainport's town crier once saved the townsfolk from predation by a rottenmouth viper with only their bell."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5c94bc0-a49d-451b-8e8d-64d46b8b8603.jpg?1721427270"
    }
}
