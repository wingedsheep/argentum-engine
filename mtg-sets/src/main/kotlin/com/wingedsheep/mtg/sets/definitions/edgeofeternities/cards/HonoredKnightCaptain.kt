package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Honored Knight-Captain
 * {1}{W}
 * Creature — Human Advisor Knight
 * When this creature enters, create a 1/1 white Human Soldier creature token.
 * {4}{W}{W}, Sacrifice this creature: Search your library for an Equipment card, put it onto the battlefield, then shuffle.
 */
val HonoredKnightCaptain = card("Honored Knight-Captain") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Advisor Knight"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, create a 1/1 white Human Soldier creature token.\n{4}{W}{W}, Sacrifice this creature: Search your library for an Equipment card, put it onto the battlefield, then shuffle."

    // ETB ability: create 1/1 white Human Soldier token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier")
        )
    }

    // Activated ability: search for Equipment
    activatedAbility {
        cost = com.wingedsheep.sdk.dsl.Costs.Composite(
            com.wingedsheep.sdk.dsl.Costs.Mana("{4}{W}{W}"),
            com.wingedsheep.sdk.dsl.Costs.SacrificeSelf
        )
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.HasSubtype(Subtype("Equipment"))
                )
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Forrest Imel"
        flavorText = "\"Hope can never fade so long as there's a hand to carry my sword when I fall.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05a6ab03-f0b9-4738-a5f9-5d95bb22de75.jpg?1752946639"
    }
}
