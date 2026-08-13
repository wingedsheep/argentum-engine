package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.RoadsGoEverEverOn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Roads Go Ever, Ever On — linked-exile chapters and the targeted attack trigger from chapter IV. */
class RoadsGoEverEverOnScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RoadsGoEverEverOn)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain(target: EntityId? = null) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 100) {
            when (state.pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(activePlayer!!, listOfNotNull(target))
                is SelectCardsDecision -> {
                    val decision = state.pendingDecision as SelectCardsDecision
                    submitCardSelection(decision.playerId, decision.options.take(decision.maxSelections))
                }
                null -> bothPass()
                else -> autoResolveDecision()
            }
            guard++
        }
    }

    fun GameTestDriver.advanceToMain(nthControllerTurn: Int) {
        val targetTurn = nthControllerTurn * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    fun GameTestDriver.castRoads(controller: EntityId): EntityId {
        giveMana(controller, Color.WHITE)
        giveColorlessMana(controller, 1)
        val saga = putCardInHand(controller, "Roads Go Ever, Ever On")
        castSpell(controller, saga).isSuccess shouldBe true
        drain()
        return saga
    }

    test("chapter I exiles up to two basic Plains linked to the Saga and gains 2 life") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val saga = driver.castRoads(controller)

        driver.state.lifeTotal(controller) shouldBe 22
        driver.state.getEntity(saga)?.get<LinkedExileComponent>()?.exiledIds?.size shouldBe 2
        driver.getExile(controller).size shouldBe 2
    }

    test("chapters II and III each return a chosen linked card to its owner's hand") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val saga = driver.castRoads(controller)

        driver.advanceToMain(2)
        driver.drain()
        driver.state.getEntity(saga)?.get<LinkedExileComponent>()?.exiledIds?.size shouldBe 1

        driver.advanceToMain(3)
        driver.drain()
        driver.state.getEntity(saga)?.get<LinkedExileComponent>()?.exiledIds shouldBe emptyList()
        driver.getExile(controller) shouldBe emptyList()
    }

    test("chapter IV makes each attack this turn target and pump a creature by the live Plains count") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        val attacker = driver.putCreatureOnBattlefield(controller, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        repeat(3) { driver.putPermanentOnBattlefield(controller, "Plains") }
        driver.castRoads(controller)

        driver.advanceToMain(2)
        driver.drain()
        driver.advanceToMain(3)
        driver.drain()
        driver.advanceToMain(4)
        driver.drain()
        driver.state.delayedTriggers.size shouldBe 1

        driver.putPermanentOnBattlefield(controller, "Plains")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(controller, listOf(attacker), opponent).error shouldBe null
        driver.drain(target = attacker)

        driver.state.projectedState.getPower(attacker) shouldBe 7
        driver.state.projectedState.getToughness(attacker) shouldBe 7
    }
})
