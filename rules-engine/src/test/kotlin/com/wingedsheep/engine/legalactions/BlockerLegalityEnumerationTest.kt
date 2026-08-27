package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The declare-blockers legal action carries the per-attacker half of block legality —
 * `validBlockersByAttacker` and `attackerMinBlockers` — read from the same `BlockPhaseManager`
 * rule `declareBlockers` enforces. The client uses it to refuse an illegal drop where the drag
 * happens; these tests pin the two down to each other so the highlight can never disagree with
 * the verdict.
 */
class BlockerLegalityEnumerationTest : FunSpec({

    val menaceBrute = card("Menace Brute") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        oracleText = "Menace"
        power = 3
        toughness = 3
        keywords(Keyword.MENACE)
    }

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + menaceBrute)
        it.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Island" to 10,
                "Grizzly Bears" to 5,
                "Wind Drake" to 5,
                "Phantom Warrior" to 5,
            )
        )
    }

    data class Board(
        val driver: GameTestDriver,
        val attacker: EntityId,
        val defender: EntityId,
        val bears: EntityId,
        val drake: EntityId,
        val warrior: EntityId,
        val brute: EntityId,
        val defendingBears: EntityId,
        val defendingDrake: EntityId,
    )

    /** Active player attacks with Bears (vanilla), Wind Drake (flying), Phantom Warrior (unblockable), Menace Brute. */
    fun attackWithEverything(): Board {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val drake = driver.putCreatureOnBattlefield(attacker, "Wind Drake")
        val warrior = driver.putCreatureOnBattlefield(attacker, "Phantom Warrior")
        val brute = driver.putCreatureOnBattlefield(attacker, "Menace Brute")
        listOf(bears, drake, warrior, brute).forEach { driver.removeSummoningSickness(it) }
        val defendingBears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        val defendingDrake = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(bears, drake, warrior, brute), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        return Board(driver, attacker, defender, bears, drake, warrior, brute, defendingBears, defendingDrake)
    }

    fun Board.blockersAction(): LegalAction =
        driver.legalActions(defender).single { it.action is DeclareBlockers }

    test("each attacker lists exactly the blockers the engine would accept for it") {
        val board = attackWithEverything()
        val action = board.blockersAction()
        val byAttacker = action.validBlockersByAttacker.shouldNotBeNull()

        // Vanilla and menace attackers: anyone may block them.
        byAttacker[board.bears].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(board.defendingBears, board.defendingDrake)
        byAttacker[board.brute].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(board.defendingBears, board.defendingDrake)
        // Flying: only the flier.
        byAttacker[board.drake].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(board.defendingDrake)
        // Unblockable: absent — no legal blocker at all.
        byAttacker shouldNotContainKey board.warrior
    }

    test("the per-attacker list and the declaration verdict agree, pair for pair") {
        val board = attackWithEverything()
        val byAttacker = board.blockersAction().validBlockersByAttacker.shouldNotBeNull()
        val blockers = listOf(board.defendingBears, board.defendingDrake)
        val attackers = listOf(board.bears, board.drake, board.warrior)
        for (blocker in blockers) for (attacker in attackers) {
            val advertised = byAttacker[attacker]?.contains(blocker) == true
            val snapshot = board.driver.state
            val result = board.driver.declareBlockers(board.defender, mapOf(blocker to listOf(attacker)))
            result.isSuccess shouldBe advertised
            board.driver.replaceState(snapshot)
        }
    }

    test("menace is reported as a minimum of two blockers, and enforced as one") {
        val board = attackWithEverything()
        val action = board.blockersAction()
        action.attackerMinBlockers.shouldNotBeNull() shouldBe mapOf(board.brute to 2)

        val single = board.driver.declareBlockers(board.defender, mapOf(board.defendingBears to listOf(board.brute)))
        single.isSuccess shouldBe false
        single.error.shouldNotBeNull() shouldContain "menace"

        val double = board.driver.declareBlockers(
            board.defender,
            mapOf(board.defendingBears to listOf(board.brute), board.defendingDrake to listOf(board.brute))
        )
        double.isSuccess shouldBe true
    }

    test("a creature that can block nothing is not a valid blocker at all") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val drake = driver.putCreatureOnBattlefield(attacker, "Wind Drake")
        driver.removeSummoningSickness(drake)
        val defendingBears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(drake), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val action = driver.legalActions(defender).single { it.action is DeclareBlockers }
        action.validBlockers.shouldNotBeNull().contains(defendingBears) shouldBe false
        action.validBlockersByAttacker shouldBe null
        action.attackerMinBlockers shouldBe null
    }
})
