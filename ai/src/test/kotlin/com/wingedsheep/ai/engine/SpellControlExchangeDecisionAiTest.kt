package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.PerplexingChimera
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "You may exchange control of this creature and that spell" (Perplexing Chimera): the AI trades
 * its creature for an opponent's creature spell only when the spell is worth more than what it
 * hands over, and the draft rater prices the ability's "if you do" payoff.
 */
class SpellControlExchangeDecisionAiTest : FunSpec({

    /** The AI (P1) holds a Chimera; on P2's turn P2 casts [creature]; returns the AI's answer to the exchange. */
    fun exchangesFor(creature: String, mana: Int): Boolean {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + PerplexingChimera)
            initMirrorMatch(Deck.of("Forest" to 30), startingPlayer = 1)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val ai = d.player1
        val opponent = d.player2
        d.putCreatureOnBattlefield(ai, "Perplexing Chimera")
        val card = d.putCardInHand(opponent, creature)
        d.giveMana(opponent, Color.GREEN, mana)
        d.submit(CastSpell(opponent, card, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass() // the Chimera trigger resolves to its "may"

        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe ai
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        return responder.respond(d.state, decision, ai).shouldBeInstanceOf<YesNoResponse>().choice
    }

    test("takes a 5/5 creature spell for its 3/3") {
        exchangesFor("Force of Nature", 5) shouldBe true
    }

    test("keeps its 3/3 rather than take a 2/2") {
        exchangesFor("Grizzly Bears", 2) shouldBe false
    }

    test("the rater prices an \"if you do\" gate's payoff, not nothing") {
        // May(IfYouDo(exchange, then = choose new targets)): the retarget is the payoff it reads.
        LimitedCardRater.effectValue(PerplexingChimera.script.triggeredAbilities.single().effect) shouldBeGreaterThan 0.0
    }
})
