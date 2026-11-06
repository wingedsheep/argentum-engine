package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.ExquisiteArchangel
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Exquisite Archangel dealt lethal damage in the same state-based-action pass that would lose its
 * controller the game: the controller chooses exile (the replacement) or the graveyard (the
 * state-based action). Either way the life total resets and the Archangel is off the board, so the
 * simulated branches score alike and the AI takes the printed replacement: exile.
 */
class GameLossSourceChoiceAiTest : FunSpec({
    val inferno = card("Lossward Inferno") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(5, t) then Effects.LoseLife(30, EffectTarget.Controller)
        }
    }

    test("the AI answers the exile-or-graveyard question with the printed exile, and stays in the game") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(ExquisiteArchangel, inferno))
            initMirrorMatch(Deck.of("Plains" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val angel = d.putCreatureOnBattlefield(player, "Exquisite Archangel")
        d.castSpell(player, d.putCardInHand(player, "Lossward Inferno"), listOf(angel)).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        val choice = responder.respond(d.state, decision, player).shouldBeInstanceOf<YesNoResponse>().choice
        choice shouldBe true
        d.submitYesNo(player, choice)
        d.state.gameOver shouldBe false
        d.getLifeTotal(player) shouldBe 20
    }
})
