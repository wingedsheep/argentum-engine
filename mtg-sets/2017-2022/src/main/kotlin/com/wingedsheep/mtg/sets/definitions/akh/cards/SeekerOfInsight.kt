package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Seeker of Insight
 * {1}{U}
 * Creature — Human Wizard
 * 1/3
 * {T}: Draw a card, then discard a card. Activate only if you've cast a noncreature spell
 * this turn.
 */
val SeekerOfInsight = card("Seeker of Insight") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: Draw a card, then discard a card. " +
        "Activate only if you've cast a noncreature spell this turn."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.Noncreature)
            )
        )
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Magali Villeneuve"
        flavorText = "The Trial of Knowledge challenges and broadens the minds of initiates."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f0b7bfc-f126-4885-a663-6f8dcc52e663.jpg?1783936517"
    }
}
