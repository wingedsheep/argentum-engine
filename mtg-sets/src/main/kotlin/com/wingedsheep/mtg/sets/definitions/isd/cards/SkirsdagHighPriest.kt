package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skirsdag High Priest
 * {1}{B}
 * Creature — Human Cleric
 * 1/2
 *
 * Morbid — {T}, Tap two untapped creatures you control: Create a 5/5 black Demon creature token
 * with flying. Activate only if a creature died this turn.
 *
 * Morbid is an ability word with no rules meaning; the "activate only if" clause is the real
 * restriction, modeled as [ActivationRestriction.OnlyIfCondition] over
 * [Conditions.CreatureDiedThisTurn] (any creature, any controller). The tap-two half excludes the
 * source (`excludeSelf = true`) — the High Priest is already being tapped by the {T} half of the
 * same cost, so it can't also be one of the two. Per the ISD ruling those two creatures need not
 * have been under your control since your most recent turn began; only the {T} half is subject to
 * summoning sickness, which the engine enforces for the tap symbol already.
 */
val SkirsdagHighPriest = card("Skirsdag High Priest") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "Morbid — {T}, Tap two untapped creatures you control: Create a 5/5 black Demon " +
        "creature token with flying. Activate only if a creature died this turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(
                count = 2,
                filter = GameObjectFilter.Creature.youControl(),
                excludeSelf = true
            )
        )
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CreatureDiedThisTurn))
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Demon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/7/7/771ae1f8-70b3-40da-8352-421a36c7abb5.jpg?1783940883"
        )
        description = "Create a 5/5 black Demon creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Jason A. Engle"
        flavorText = "\"Thraben's pleas fall on deaf ears. Ours do not.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09aa6b66-f69b-4f89-b802-e30c247f90e3.jpg?1783940949"

        ruling(
            "2020-08-07",
            "Unlike Skirsdag High Priest itself, the two other creatures you tap to activate its " +
                "ability aren't required to have been under your control continuously since the " +
                "beginning of your most recent turn."
        )
    }
}
