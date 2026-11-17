package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tapestry of the Ages
 * {4}
 * Artifact
 * {2}, {T}: Draw a card. Activate only if you've cast a noncreature spell this turn.
 */
val TapestryOfTheAges = card("Tapestry of the Ages") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Draw a card. Activate only if you've cast a noncreature spell this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.Noncreature)
            )
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Yeong-Hao Han"
        flavorText = "\"Abzan, Jeskai, Sultai, Mardu, Temur—names lost to history, yet worthy of further study.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/122e5e24-d704-4f1c-9577-0f25aaabb7ed.jpg?1783938567"
    }
}
