package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Royal Scions — Throne of Eldraine #199 (canonical printing)
 * {1}{U}{R} · Legendary Planeswalker — Will Rowan · Loyalty 5
 *
 * +1: Draw a card, then discard a card.
 * +1: Target creature gets +2/+0 and gains first strike and trample until end of turn.
 * −8: Draw four cards. When you do, The Royal Scions deals damage to any target equal to the
 *     number of cards in your hand.
 *
 * The −8's "when you do" is a CR 603.12 reflexive trigger: its target is chosen after the four
 * cards are seen, and the damage reads the hand size as that trigger resolves. Per the rulings it
 * triggers only if all four cards were actually drawn (a draw cap such as Narset, Parter of Veils
 * or a replaced draw stops it), and only once even if replacement effects draw more — so it is
 * gated on the growth of the controller's cards-drawn-this-turn tally across the draw being at
 * least four, with the Hidetsugu, Devouring Chaos shape `If(cond, ReflexiveTrigger(Nothing, …))`.
 */
val TheRoyalScions = card("The Royal Scions") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Will Rowan"
    startingLoyalty = 5
    oracleText = "+1: Draw a card, then discard a card.\n" +
        "+1: Target creature gets +2/+0 and gains first strike and trample until end of turn.\n" +
        "−8: Draw four cards. When you do, The Royal Scions deals damage to any target equal to " +
        "the number of cards in your hand."

    // +1: Draw a card, then discard a card.
    loyaltyAbility(+1) {
        effect = Patterns.Hand.loot()
    }

    // +1: Target creature gets +2/+0 and gains first strike and trample until end of turn.
    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    // −8: Draw four cards. When you do, deal damage equal to cards in hand to any target.
    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val drawnThisTurn = DynamicAmounts.cardsDrawnThisTurn()
            val drawnBefore = storeNumber(drawnThisTurn)
            run(Effects.DrawCards(4))
            run(
                Effects.If(
                    condition = Conditions.CompareAmounts(
                        drawnThisTurn - drawnBefore.amount,
                        ComparisonOperator.GTE,
                        4,
                    ),
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        descriptionOverride = "The Royal Scions deals damage to any target equal to " +
                            "the number of cards in your hand."
                    ) {
                        val anyTarget = target(Targets.Any)
                        effect = Effects.DealDamage(DynamicAmounts.cardsInYourHand(), anyTarget)
                    }
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a7111f3-01a6-4311-bc08-036a1fba60f5.jpg?1783932595"
        ruling("2019-10-04", "You draw a card and discard a card all while The Royal Scions's first ability is resolving. Nothing can happen between the two, and no player may choose to take actions.")
        ruling("2019-10-04", "The Royal Scions's last ability goes on the stack without a target. While that ability is resolving, you draw four cards. When you do, the reflexive triggered ability triggers and you pick a target to be dealt damage. This is different from effects that say “If you do . . .” in that you choose a target after looking at the cards you draw.")
        ruling("2019-10-04", "If you draw fewer than four cards while resolving the last ability of The Royal Scions, its reflexive triggered ability doesn't trigger.")
        ruling("2019-10-04", "If replacement effects cause you to draw five or more cards while resolving the last ability of The Royal Scions, its reflexive triggered ability still triggers only once, even if you draw eight cards.")
    }
}
