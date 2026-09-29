package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zimone and Dina
 * {B}{G}{U}
 * Legendary Creature — Human Dryad
 * 3/4
 * Whenever you draw your second card each turn, target opponent loses 2 life and you gain 2 life.
 * {T}, Sacrifice another creature: Draw a card. You may put a land card from your hand onto the
 * battlefield tapped. If you control eight or more lands, repeat this process once.
 *
 * "Repeat this process once" re-runs the draw + optional land drop exactly once (not the cost), and
 * the eight-lands check is made after the first pass — so the first pass's land counts toward it.
 */
private val zimoneProcess =
    Effects.DrawCards(1) then
        Patterns.Hand.putFromHand(GameObjectFilter.Land, count = 1, entersTapped = true)

val ZimoneAndDina = card("Zimone and Dina") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Human Dryad"
    power = 3
    toughness = 4
    oracleText = "Whenever you draw your second card each turn, target opponent loses 2 life and you gain 2 life.\n" +
        "{T}, Sacrifice another creature: Draw a card. You may put a land card from your hand onto the " +
        "battlefield tapped. If you control eight or more lands, repeat this process once."

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.DrainLife(
            amount = 2,
            from = opponent,
            to = EffectTarget.Controller,
        )
        description = "Whenever you draw your second card each turn, target opponent loses 2 life and you gain 2 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = zimoneProcess then
            Effects.If(Conditions.ControlLandsAtLeast(8), zimoneProcess)
        description = "{T}, Sacrifice another creature: Draw a card. You may put a land card from your hand onto " +
            "the battlefield tapped. If you control eight or more lands, repeat this process once."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "257"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2af874-1052-4cad-90ed-d80e49d4c68c.jpg?1783916936"
        ruling(
            "2023-04-14",
            "Zimone and Dina doesn't need to have been under your control when the first card is drawn for its " +
                "ability to trigger. As long as you control it when you draw your second card in a turn, that " +
                "ability will trigger."
        )
        ruling(
            "2023-04-14",
            "For the activated ability, repeating the process means the entire effect, but not the cost. You " +
                "don't have to (and, in fact, can't) sacrifice another creature. You'll draw another card, then " +
                "you may put another land card from your hand onto the battlefield tapped. As this is your second " +
                "time through the process, the ability will finish resolving."
        )
    }
}
