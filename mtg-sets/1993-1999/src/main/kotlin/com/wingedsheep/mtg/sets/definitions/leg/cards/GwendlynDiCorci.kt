package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Gwendlyn Di Corci
 * {U}{B}{B}{R}
 * Legendary Creature — Human Rogue
 * 3/5
 * {T}: Target player discards a card at random. Activate only during your turn.
 */
val GwendlynDiCorci = card("Gwendlyn Di Corci") {
    manaCost = "{U}{B}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Rogue"
    oracleText = "{T}: Target player discards a card at random. Activate only during your turn."
    power = 3
    toughness = 5

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardRandom(1, player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Julie Baroh"
        flavorText = "Urborg became the final resting place of many swordsmen of renown, their steel " +
            "outmatched by the wiles of Gwendlyn."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/473d70b6-a88c-49f4-9415-19919c4468ae.jpg?1783948039"
    }
}
