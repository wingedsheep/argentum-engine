package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Recipient.AnotherPlayer] — "another player" — is every player except the observing ability's
 * controller. It is wider than [Recipient.Opponent]: in a team game a teammate is another player but
 * not an opponent (CR 102.3). It never matches an object.
 */
class AnotherPlayerRecipientTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun GameTestDriver.matches(entity: EntityId, recipient: Recipient, controller: EntityId): Boolean =
        evaluator.matchesRecipient(
            state, state.projectedState, entity, recipient, PredicateContext(controllerId = controller),
        )

    test("in a duel it is exactly the opponent") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 40))

        d.matches(d.player2, Recipient.AnotherPlayer, d.player1) shouldBe true
        d.matches(d.player1, Recipient.AnotherPlayer, d.player1) shouldBe false
    }

    test("it never matches an object, even one another player controls") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 40))
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.matches(bears, Recipient.AnotherPlayer, d.player1) shouldBe false
    }

    test("in Two-Headed Giant it covers the teammate that an opponent test skips") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        val (you, teammate, foe1, foe2) = d.initMultiplayer(
            decks = List(4) { Deck.of("Island" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )

        withClue("teammate: another player, not an opponent") {
            d.matches(teammate, Recipient.AnotherPlayer, you) shouldBe true
            d.matches(teammate, Recipient.Opponent, you) shouldBe false
        }
        withClue("both opposing players are another player") {
            d.matches(foe1, Recipient.AnotherPlayer, you) shouldBe true
            d.matches(foe2, Recipient.AnotherPlayer, you) shouldBe true
        }
        d.matches(you, Recipient.AnotherPlayer, you) shouldBe false
    }
})
