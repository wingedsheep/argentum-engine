package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.ValleyDasher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for Valley Dasher's "attacks each combat if able" with haste.
 *
 * Valley Dasher: {1}{R}
 * Creature — Human Berserker
 * 2/2
 * Haste
 * Valley Dasher attacks each combat if able.
 */
class ValleyDasherTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ValleyDasher)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            skipMulligans = true
        )
        return driver
    }

    test("Valley Dasher with haste must attack the turn it enters the battlefield") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Valley Dasher on battlefield (still has summoning sickness, but has haste)
        val dasherId = driver.putCreatureOnBattlefield(activePlayer, "Valley Dasher")

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Try to declare no attackers - should fail because Valley Dasher must attack
        val noAttackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        noAttackResult.isSuccess shouldBe false
        noAttackResult.error shouldContain "must attack"

        // Declare Valley Dasher as attacker - should succeed
        val attackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(dasherId to opponent)
            )
        )
        attackResult.isSuccess shouldBe true
    }

    test("Valley Dasher without summoning sickness must attack") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dasherId = driver.putCreatureOnBattlefield(activePlayer, "Valley Dasher")
        driver.removeSummoningSickness(dasherId)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val noAttackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        noAttackResult.isSuccess shouldBe false
        noAttackResult.error shouldContain "must attack"
    }
})
