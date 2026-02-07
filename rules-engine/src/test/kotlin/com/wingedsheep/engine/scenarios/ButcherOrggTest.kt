package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Butcher Orgg's "divide combat damage freely" ability.
 *
 * Butcher Orgg is a 6/6 creature that can assign its combat damage divided
 * as its controller chooses among the defending player and/or any creatures
 * they control. Auto-assignment: lethal to each blocker in order, remainder
 * to defending player.
 */
class ButcherOrggTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("unblocked Butcher Orgg deals full damage to defending player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orgg = driver.putCreatureOnBattlefield(activePlayer, "Butcher Orgg")
        driver.removeSummoningSickness(orgg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(orgg), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Unblocked 6/6 deals 6 damage to opponent
        driver.assertLifeTotal(opponent, 14)
    }

    test("blocked Butcher Orgg deals excess damage to defending player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orgg = driver.putCreatureOnBattlefield(activePlayer, "Butcher Orgg")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(orgg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(orgg), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(orgg))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 2/2 blocker should be dead (received 2 lethal damage)
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"

        // Remaining 4 damage goes to defending player (6 - 2 = 4)
        driver.assertLifeTotal(opponent, 16)

        // Butcher Orgg takes 2 damage from blocker but survives (6 toughness)
        driver.findPermanent(activePlayer, "Butcher Orgg") shouldNotBe null
    }

    test("blocked by multiple creatures, excess goes to player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orgg = driver.putCreatureOnBattlefield(activePlayer, "Butcher Orgg")
        val blocker1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")  // 2/2
        val blocker2 = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")   // 2/1
        driver.removeSummoningSickness(orgg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(orgg), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(
            blocker1 to listOf(orgg),
            blocker2 to listOf(orgg)
        ))

        // Multiple blockers require damage assignment order decision
        val decision = driver.pendingDecision as OrderObjectsDecision
        driver.submitDecision(
            activePlayer,
            OrderedResponse(decision.id, listOf(blocker1, blocker2))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both blockers should be dead
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Goblin Guide") shouldBe null

        // Remaining damage to player: 6 - 2 (bears lethal) - 1 (goblin lethal) = 3
        driver.assertLifeTotal(opponent, 17)
    }

    test("blocked with no excess - all damage to blocker, none to player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orgg = driver.putCreatureOnBattlefield(activePlayer, "Butcher Orgg")
        val bigBlocker = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5
        driver.removeSummoningSickness(orgg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(orgg), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(bigBlocker to listOf(orgg))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 5/5 blocker needs 5 lethal damage; Orgg only has 6 power, so 1 excess to player
        driver.assertLifeTotal(opponent, 19)
    }

    test("blockers still deal damage back to Butcher Orgg") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orgg = driver.putCreatureOnBattlefield(activePlayer, "Butcher Orgg")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(orgg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(orgg), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(orgg))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 3/3 blocker dies (3 lethal), remaining 3 to player
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.assertLifeTotal(opponent, 17)

        // Butcher Orgg survives with 3 damage marked (6 toughness - 3 damage)
        driver.findPermanent(activePlayer, "Butcher Orgg") shouldNotBe null
    }
})
