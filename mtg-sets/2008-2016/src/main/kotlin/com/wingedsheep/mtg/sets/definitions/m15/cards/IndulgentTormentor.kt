package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Indulgent Tormentor
 * {3}{B}{B}
 * Creature — Demon
 * 5/3
 *
 * Flying
 * At the beginning of your upkeep, draw a card unless target opponent sacrifices a creature of
 * their choice or pays 3 life.
 *
 * The target opponent is the payer of a [com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect]
 * whose cost is a [Costs.pay.Choice] of sacrificing a creature or paying 3 life; the "suffer" is
 * the Tormentor's controller drawing a card (the suffer runs in the ability's own context, so the
 * draw lands on you). Options the opponent can't pay (no creatures, under 3 life) are dropped, so
 * they can't make an impossible choice; if the opponent stops being a legal target the whole
 * ability fizzles and nobody draws.
 */
val IndulgentTormentor = card("Indulgent Tormentor") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 3
    oracleText = "Flying\nAt the beginning of your upkeep, draw a card unless target opponent sacrifices a " +
        "creature of their choice or pays 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val opponent = target(Targets.Opponent)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Choice(
                listOf(
                    Costs.pay.Sacrifice(filter = GameObjectFilter.Creature),
                    Costs.pay.PayLife(3)
                )
            ),
            suffer = Effects.DrawCards(1),
            player = opponent,
            consequenceDescription = "let Indulgent Tormentor's controller draw a card",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Wesley Burt"
        flavorText = "The promise of anguish is payment enough for services rendered."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4da8a4d8-6d18-4588-8da9-cd32658071f0.jpg?1783939183"

        ruling("2017-11-17", "The opponent chooses whether they sacrifice a creature, pays 3 life, or allows you to draw a card. That player can't make an impossible choice, such as sacrificing a creature while they control no creatures.")
        ruling("2017-11-17", "If there are no legal targets to choose, perhaps because all your opponents have hexproof, the ability is removed from the stack with no effect. You won't draw a card. This is also true if the target opponent becomes an illegal target in response to the ability.")
    }
}
