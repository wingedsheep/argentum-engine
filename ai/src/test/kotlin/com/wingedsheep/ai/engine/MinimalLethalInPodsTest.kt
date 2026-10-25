package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.minimalLethalInPods]: a lethal attack in a pod sends only what the kill needs.
 *
 * Off a 4-player AI-vs-AI playtest (2026-10-25, game 01, turn 38): Seat1 sent all six attackers at
 * Seat2 on 2 life with no flying blockers, when two Drakes or the Griffin alone were enough. The
 * kill landed, the board was tapped out, and on turn 40 Seat0 swung 21 power into it — Seat1 died
 * at −7. The lethal seed is returned as-is (no local search, no crack-back pricing), which is right
 * in 1v1, where the game ends, and wrong while another opponent is still at the table.
 */
class MinimalLethalInPodsTest : FunSpec({

    val cards = TestCards.all
    val registry = CardRegistry().apply { register(cards) }

    fun advisor(minimalLethalInPods: Boolean) = CombatAdvisor(
        GameSimulator(registry), AIPlayer.defaultEvaluator(), registry,
        minimalLethalInPods = minimalLethalInPods,
    )

    fun GameTestDriver.readyAttacker(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    class Board(
        val driver: GameTestDriver,
        val me: EntityId,
        val fliers: List<EntityId>,
        val ground: List<EntityId>,
        val vigilant: List<EntityId>,
    )

    /**
     * We have two Wind Drakes and four Hill Giants. The seat at 2 life has two Grizzly Bears (no
     * flying, no reach); in a pod, a third seat has four untapped Hill Giants — 12 power against
     * our 8 life, lethal if we tap out and survivable with the Giants home. [vigilantAttackers]
     * Ardent Militias (2/5 vigilance) join our side.
     */
    fun board(players: Int, vigilantAttackers: Int = 0): Board {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        val seats = driver.initMultiplayer(decks = List(players) { Deck.of("Plains" to 40) })
        val me = seats[0]
        val dying = seats[1]
        val fliers = List(2) { driver.readyAttacker(me, "Wind Drake") }
        val ground = List(4) { driver.readyAttacker(me, "Hill Giant") }
        val vigilant = List(vigilantAttackers) { driver.readyAttacker(me, "Ardent Militia") }
        repeat(2) { driver.readyAttacker(dying, "Grizzly Bears") }
        driver.replaceState(driver.state.withLifeTotal(dying, 2).withLifeTotal(me, 8))
        if (players > 2) {
            repeat(4) { driver.readyAttacker(seats[2], "Hill Giant") }
        }
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        return Board(driver, me, fliers, ground, vigilant)
    }

    fun Board.declare(advisor: CombatAdvisor): DeclareAttackers {
        val action = driver.legalActions(me).single { it.actionType == "DeclareAttackers" }
        return advisor.chooseAttackers(driver.state, action, me) as DeclareAttackers
    }

    test("in a pod, a lethal attack sends both Drakes (lethal with one to spare) and keeps the Giants home") {
        val board = board(players = 3)
        val plan = board.declare(advisor(minimalLethalInPods = true))

        // One Drake is exact lethal, which a single removal spell or flash blocker stops; the
        // second is the margin. The Giants are the crack-back defence and stay home.
        plan.attackers.keys shouldContainExactlyInAnyOrder board.fliers
        board.driver.submitSuccess(plan)
    }

    test("in a pod, a vigilant attacker is never trimmed: attacking costs it nothing on defence") {
        val board = board(players = 3, vigilantAttackers = 1)
        val plan = board.declare(advisor(minimalLethalInPods = true))

        plan.attackers.keys shouldContainExactlyInAnyOrder board.fliers + board.vigilant
        board.driver.submitSuccess(plan)
    }

    test("in 1v1, the lethal alpha strike still sends everything") {
        val board = board(players = 2)
        val plan = board.declare(advisor(minimalLethalInPods = true))

        plan.attackers.keys shouldContainExactlyInAnyOrder board.fliers + board.ground
        board.driver.submitSuccess(plan)
    }

    test("with the flag off, the pod alpha strike sends everything (the legacy behaviour)") {
        val board = board(players = 3)
        val plan = board.declare(advisor(minimalLethalInPods = false))

        plan.attackers.keys shouldContainExactlyInAnyOrder board.fliers + board.ground
    }

    test("LIVE ships the flag") {
        AiProfile.LIVE.minimalLethalInPods shouldBe true
    }
})
