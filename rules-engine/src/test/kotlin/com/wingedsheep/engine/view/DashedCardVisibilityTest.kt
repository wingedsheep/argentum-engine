package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A permanent cast for its dash cost (CR 702.109, Khans of Tarkir) carries a
 * [com.wingedsheep.engine.state.components.battlefield.DashedComponent] until it's returned to its
 * owner's hand at the beginning of the next end step. [ClientStateTransformer] surfaces that as
 * [ClientCard.isDashed] so the client can show the dash cue — otherwise the permanent is visually
 * indistinguishable from one cast for its regular cost. The flag is public (dash casting is visible
 * to all players), so both the controller and the opponent see it. Mirrors [WarpedCardVisibilityTest].
 */
class DashedCardVisibilityTest : FunSpec({

    val dashCreature = card("Dash Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental"
        power = 4
        toughness = 3
        dash = "{1}{R}"
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + dashCreature)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    test("a creature cast for its regular cost is not flagged isDashed") {
        val d = driver()
        val player = d.activePlayer!!
        val cardId = d.putCardInHand(player, "Dash Test Creature")
        d.giveMana(player, Color.RED, 5)

        d.submit(CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()

        val permanent = d.findPermanent(player, "Dash Test Creature").shouldNotBeNull()
        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[permanent].shouldNotBeNull().isDashed shouldBe false
    }

    test("a creature cast for its dash cost is flagged isDashed for both players") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val cardId = d.putCardInHand(player, "Dash Test Creature")
        d.giveMana(player, Color.RED, 2) // dash cost {1}{R}

        d.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        d.bothPass()

        val permanent = d.findPermanent(player, "Dash Test Creature").shouldNotBeNull()

        val ownerView = transformer(d).transform(d.state, viewingPlayerId = player)
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = opponent)

        ownerView.cards[permanent].shouldNotBeNull().isDashed shouldBe true
        opponentView.cards[permanent].shouldNotBeNull().isDashed shouldBe true
    }
})
