package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.AtzocanArcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OptionalFightDecisionAiTest : FunSpec({
    data class Setup(val driver: GameTestDriver, val player: EntityId, val target: EntityId)

    fun targetFight(): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + AtzocanArcher)
            initMirrorMatch(Deck.of("Forest" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val target = d.putCreatureOnBattlefield(d.getOpponent(player), "Llanowar Elves")
        val archer = d.putCardInHand(player, "Atzocan Archer")
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, archer).error shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(player, listOf(target)).error shouldBe null
        d.pendingDecision shouldBe null
        return Setup(d, player, target)
    }

    fun answer(d: GameTestDriver, player: EntityId): Boolean {
        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        return responder.respond(d.state, decision, player).shouldBeInstanceOf<YesNoResponse>().choice
    }

    test("accepts an optional fight that kills the locked target and preserves its creature") {
        val (d, player) = targetFight()
        d.bothPass()
        answer(d, player) shouldBe true
    }

    test("declines the same locked fight after an opponent pumps the target in response") {
        val (d, player, target) = targetFight()
        val opponent = d.getOpponent(player)
        d.passPriority(player).error shouldBe null
        val growth = d.putCardInHand(opponent, "Giant Growth")
        d.giveMana(opponent, Color.GREEN)
        d.castSpell(opponent, growth, listOf(target)).error shouldBe null
        d.bothPass()
        d.pendingDecision shouldBe null
        d.bothPass()
        answer(d, player) shouldBe false
    }
})
