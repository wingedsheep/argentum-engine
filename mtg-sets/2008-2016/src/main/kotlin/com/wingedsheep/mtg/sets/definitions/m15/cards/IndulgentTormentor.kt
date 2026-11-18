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
 * Indulgent Tormentor — Magic 2015 #101
 * {3}{B}{B} · Creature — Demon · 5/3
 *
 * Flying
 * At the beginning of your upkeep, draw a card unless target opponent sacrifices a creature of
 * their choice or pays 3 life.
 *
 * A routed [Costs.pay.Choice]: the targeted opponent picks one of the two costs or lets the
 * Tormentor's controller draw. The draw always goes to the controller, whichever question the
 * opponent declines, and they may pay 3 life at exactly 3 (CR 119.4) — then lose the game.
 */
val IndulgentTormentor = card("Indulgent Tormentor") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 3
    oracleText = "Flying\nAt the beginning of your upkeep, draw a card unless target opponent " +
        "sacrifices a creature of their choice or pays 3 life."

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
            consequenceDescription = "let Indulgent Tormentor's controller draw a card"
        )
        description = "At the beginning of your upkeep, draw a card unless target opponent " +
            "sacrifices a creature of their choice or pays 3 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Wesley Burt"
        flavorText = "The promise of anguish is payment enough for services rendered."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4da8a4d8-6d18-4588-8da9-cd32658071f0.jpg?1783939183"
    }
}
