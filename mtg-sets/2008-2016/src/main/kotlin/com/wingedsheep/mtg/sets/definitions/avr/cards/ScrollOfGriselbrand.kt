package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scroll of Griselbrand
 * {1}
 * Artifact
 *
 * {1}, Sacrifice this artifact: Target opponent discards a card. If you control a Demon,
 * that player loses 3 life.
 */
val ScrollOfGriselbrand = card("Scroll of Griselbrand") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, Sacrifice this artifact: Target opponent discards a card. If you control a Demon, " +
        "that player loses 3 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent) then Effects.If(
            condition = Conditions.ControlPermanentOfType(Subtype.DEMON),
            then = Effects.LoseLife(3, opponent),
        )
        description = "{1}, Sacrifice this artifact: Target opponent discards a card. If you control a Demon, " +
            "that player loses 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Cliff Childs"
        flavorText = "Words no eyes should see, telling of things no sane mind could fathom."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2263ceaf-49d2-40fe-86b8-146271b11e46.jpg?1783940650"
        ruling("2012-05-01", "Whether you control a Demon is checked when the ability resolves.")
    }
}
