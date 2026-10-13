package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SecretChoiceContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.effects.SecretChoiceEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class SecretChoiceDecisionTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("bounded secret numbers are independent of the opponent's committed number") {
        val d = driver()
        val effect = SecretChoiceEffect(choice = SecretChoice.Number(
            minValue = 1, maxValue = DynamicAmount.Fixed(3),
            matchedEffect = Effects.GainLife(10), unmatchedEffect = Effects.LoseLife(10),
        ))
        fun withHiddenNumber(number: Int) = DecisionHandler().createNumberDecision(
            state = d.state, playerId = d.player2, sourceId = null, sourceName = null,
            prompt = "Secretly choose a number", minValue = 1, maxValue = 3,
            answer = SecretChoiceContinuation(effect, EffectContext(null, d.player1), d.player2,
                remainingPlayers = emptyList(), numbers = mapOf(d.player1 to number)),
        ).state
        val one = withHiddenNumber(1)
        val three = withHiddenNumber(3)
        val first = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator(), secretChoiceRandom = Random(42))
        val second = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator(), secretChoiceRandom = Random(42))
        repeat(30) {
            val a = first.respond(one, one.pendingDecision!!, d.player2) as NumberChosenResponse
            val b = second.respond(three, three.pendingDecision!!, d.player2) as NumberChosenResponse
            a.number shouldBe b.number
            (a.number in 1..3) shouldBe true
        }
    }

    test("broad-range ranked secret bids stay at the floor instead of risking lethal life loss") {
        val d = driver()
        val effect = SecretChoiceEffect(choice = SecretChoice.Number(
            highestEffect = Effects.LoseLife(DynamicAmount.XValue),
        ))
        val state = DecisionHandler().createNumberDecision(
            state = d.state, playerId = d.player2, sourceId = null, sourceName = null,
            prompt = "Secretly choose a number", minValue = 0, maxValue = 99,
            answer = SecretChoiceContinuation(effect, EffectContext(null, d.player1), d.player2,
                remainingPlayers = emptyList(), numbers = mapOf(d.player1 to 19)),
        ).state
        val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator(), secretChoiceRandom = Random(42))
        val response = responder.respond(state, state.pendingDecision!!, d.player2) as NumberChosenResponse
        response.number shouldBe 0
    }

    test("secret creature ballots ignore other votes and prefer an opposing creature") {
        val d = driver()
        val enemy = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val own = d.putPermanentOnBattlefield(d.player2, "Serra Angel")
        val effect = SecretChoiceEffect(choice = SecretChoice.Entity("pool", storeMostChosen = "winners"))
        fun withHiddenVote(vote: com.wingedsheep.sdk.model.EntityId?) = DecisionHandler().createCardSelectionDecision(
            state = d.state, playerId = d.player2, sourceId = null, sourceName = null,
            prompt = "Secretly choose a permanent", options = listOf(enemy, own), minSelections = 0, maxSelections = 1,
            answer = SecretChoiceContinuation(effect, EffectContext(null, d.player1), d.player2,
                remainingPlayers = emptyList(), entities = mapOf(d.player1 to vote)),
        ).state
        val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator())
        for (vote in listOf(enemy, own, null)) {
            val state = withHiddenVote(vote)
            val response = responder.respond(state, state.pendingDecision!!, d.player2) as CardsSelectedResponse
            response.selectedCards shouldBe listOf(enemy)
        }
    }

    test("optional secret creature ballots abstain when only our own creatures are eligible") {
        val d = driver()
        val own = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val effect = SecretChoiceEffect(choice = SecretChoice.Entity("pool", storeMostChosen = "winners"))
        val state = DecisionHandler().createCardSelectionDecision(
            state = d.state, playerId = d.player2, sourceId = null, sourceName = null,
            prompt = "Secretly choose a permanent", options = listOf(own), minSelections = 0, maxSelections = 1,
            answer = SecretChoiceContinuation(effect, EffectContext(null, d.player1), d.player2, remainingPlayers = emptyList()),
        ).state
        val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator())
        val response = responder.respond(state, state.pendingDecision!!, d.player2) as CardsSelectedResponse
        response.selectedCards shouldBe emptyList()
    }
})
