package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val DisruptingScepter = card("Disrupting Scepter") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target player discards a card. Activate only during your turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardCards(1, player)
        description = "{3}, {T}: Target player discards a card. Activate only during your turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca571ee8-07a2-43b8-9acf-89cbfd3cf7c9.jpg?1783948667"
        ruling("2004-10-04", "You can use it on yourself.")
    }
}
