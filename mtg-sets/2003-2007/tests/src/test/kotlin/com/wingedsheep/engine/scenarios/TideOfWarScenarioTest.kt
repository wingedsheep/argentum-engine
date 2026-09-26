package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.TideOfWar
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tide of War — {4}{R}{R} Enchantment.
 * Whenever one or more creatures block, flip a coin. If you win the flip, each blocking creature
 * is sacrificed by its controller. If you lose the flip, each blocked creature is sacrificed by
 * its controller.
 *
 * The flip isn't seeded: each outcome test replays the setup until the flip lands its way and
 * reads the [CoinFlipEvent] to know which branch it is checking.
 */
class TideOfWarScenarioTest : FunSpec({

    data class Board(
        val driver: GameTestDriver,
        val active: EntityId,
        val defender: EntityId,
        val blockedAttacker: EntityId,
        val unblockedAttacker: EntityId,
        val blockers: List<EntityId>,
    )

    /**
     * Tide of War on the defender's side; two attackers, one of them double-blocked, the other
     * unblocked. Returns after blockers are declared, with the trigger on the stack.
     */
    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TideOfWar)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(defender, "Tide of War")
        val blockedAttacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val unblockedAttacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val blockers = listOf(
            driver.putCreatureOnBattlefield(defender, "Grizzly Bears"),
            driver.putCreatureOnBattlefield(defender, "Grizzly Bears"),
        )
        (blockers + blockedAttacker + unblockedAttacker).forEach(driver::removeSummoningSickness)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(blockedAttacker, unblockedAttacker), defender)
        driver.bothPass()
        driver.declareBlockers(defender, blockers.associateWith { listOf(blockedAttacker) })
        return Board(driver, active, defender, blockedAttacker, unblockedAttacker, blockers)
    }

    /** Replays [setup] until Tide of War's flip comes out [won], then runs [check]. */
    fun whenFlip(won: Boolean, check: (Board) -> Unit) {
        repeat(60) {
            val board = setup()
            val flip = board.driver.bothPass().events.filterIsInstance<CoinFlipEvent>().single()
            if (flip.won == won) {
                check(board)
                return
            }
        }
        error("never saw a ${if (won) "won" else "lost"} flip in 60 tries")
    }

    test("two blockers put a single trigger on the stack, flipped by Tide of War's controller") {
        val board = setup()
        board.driver.stackSize shouldBe 1
        val flip = board.driver.bothPass().events.filterIsInstance<CoinFlipEvent>().single()
        flip.playerId shouldBe board.defender
    }

    test("no blockers: no trigger") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TideOfWar)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(defender, "Tide of War")
        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), defender)
        driver.bothPass()
        driver.declareBlockers(defender, emptyMap())
        driver.stackSize shouldBe 0
    }

    test("won flip: every blocking creature is sacrificed; the attacker stays blocked") {
        whenFlip(won = true) { b ->
            b.blockers.forEach { (it in b.driver.state.getBattlefield()).shouldBeFalse() }
            b.driver.getGraveyardCardNames(b.defender).count { it == "Grizzly Bears" } shouldBe 2
            b.driver.state.getEntity(b.blockedAttacker)!!.has<BlockedComponent>().shouldBeTrue()

            // The blocked attacker deals no damage to the player; only the unblocked one connects.
            val life = b.driver.getLifeTotal(b.defender)
            b.driver.passPriorityUntil(Step.END_COMBAT)
            b.driver.getLifeTotal(b.defender) shouldBe life - 2
        }
    }

    test("lost flip: the blocked attacker is sacrificed; the unblocked attacker and blockers survive") {
        whenFlip(won = false) { b ->
            (b.blockedAttacker in b.driver.state.getBattlefield()).shouldBeFalse()
            b.driver.getGraveyardCardNames(b.active).count { it == "Grizzly Bears" } shouldBe 1
            (b.unblockedAttacker in b.driver.state.getBattlefield()).shouldBeTrue()
            b.blockers.forEach { (it in b.driver.state.getBattlefield()).shouldBeTrue() }
        }
    }
})
