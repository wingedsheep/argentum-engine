package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Elanor Gardner
 * {3}{G}
 * Legendary Creature — Halfling Scout
 * 2/4
 * When Elanor enters, create a Food token.
 * At the beginning of your end step, if you sacrificed a Food this turn, you may search your library
 * for a basic land card, put that card onto the battlefield tapped, then shuffle.
 */
val ElanorGardner = card("Elanor Gardner") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Halfling Scout"
    power = 2
    toughness = 4
    oracleText = "When Elanor enters, create a Food token.\n" +
        "At the beginning of your end step, if you sacrificed a Food this turn, you may search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.SacrificedFoodThisTurn
        effect = MayEffect(
            LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "286"
        artist = "Torgeir Fjereide"
        flavorText = "Her father Samwise gave her the Red Book for safekeeping before he sailed into the West."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6165eb73-49a8-4337-aa5d-7d9d48b916c9.jpg?1719684290"
    }
}
