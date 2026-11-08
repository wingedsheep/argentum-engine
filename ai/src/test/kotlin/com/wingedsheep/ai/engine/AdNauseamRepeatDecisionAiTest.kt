package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ala.cards.AdNauseam
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ad Nauseam's "you may repeat this process" is a yes/no after every reveal, and each yes costs
 * life equal to the next card. The AI must stop before the next card can kill it, and keep going
 * while the cards are free.
 */
class AdNauseamRepeatDecisionAiTest : FunSpec({

    /** Cast Ad Nauseam at [life] with [next] on top of the library; returns the driver at the first repeat prompt. */
    fun atFirstRepeatPrompt(life: Int, first: String, next: String): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + AdNauseam)
            initMirrorMatch(Deck.of("Forest" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        d.setLifeTotal(player, life)
        d.putCardOnTopOfLibrary(player, next)
        d.putCardOnTopOfLibrary(player, first)
        val spell = d.putCardInHand(player, "Ad Nauseam")
        d.giveMana(player, Color.BLACK, 5)
        d.castSpell(player, spell).error shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        return d to player
    }

    fun repeats(d: GameTestDriver, player: EntityId): Boolean {
        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        return responder.respond(d.state, decision, player).shouldBeInstanceOf<YesNoResponse>().choice
    }

    test("stops when the next card would take its life to zero") {
        val (d, player) = atFirstRepeatPrompt(life = 2, first = "Llanowar Elves", next = "Llanowar Elves")
        d.getLifeTotal(player) shouldBe 1
        repeats(d, player) shouldBe false
    }

    test("keeps revealing while the cards cost nothing") {
        val (d, player) = atFirstRepeatPrompt(life = 20, first = "Forest", next = "Forest")
        d.getLifeTotal(player) shouldBe 20
        repeats(d, player) shouldBe true
    }
})
