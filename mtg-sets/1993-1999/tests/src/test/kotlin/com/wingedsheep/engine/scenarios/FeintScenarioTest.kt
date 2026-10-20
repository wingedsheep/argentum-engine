package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.Feint
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Feint (Legends).
 *
 * "Creatures blocking target attacking creature" is a group filter that names the spell's bound
 * target, so the board has two blocked attackers: only the targeted one's blocker is tapped, and only
 * that pair's combat damage is prevented — the other pair still trades.
 */
class FeintScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Feint)
        return driver
    }

    test("taps the target's blockers and prevents combat damage by the target and its blockers only") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feinted = driver.putCreatureOnBattlefield(alice, "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(alice, "Grizzly Bears")
        driver.removeSummoningSickness(feinted)
        driver.removeSummoningSickness(other)
        val feintedBlocker = driver.putCreatureOnBattlefield(bob, "Grizzly Bears")
        val otherBlocker = driver.putCreatureOnBattlefield(bob, "Grizzly Bears")
        val feint = driver.putCardInHand(bob, "Feint")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(feinted, other), bob).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(feintedBlocker to listOf(feinted), otherBlocker to listOf(other))).error shouldBe null

        driver.giveMana(bob, Color.RED, 1)
        driver.castSpell(bob, feint, listOf(feinted)).error shouldBe null
        driver.bothPass()

        withClue("only the creature blocking the target is tapped") {
            driver.isTapped(feintedBlocker) shouldBe true
            driver.isTapped(otherBlocker) shouldBe false
        }

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val battlefield = driver.state.getBattlefield()
        withClue("the feinted attacker and its blocker deal no combat damage, so both survive") {
            battlefield.contains(feinted) shouldBe true
            battlefield.contains(feintedBlocker) shouldBe true
        }
        withClue("the other pair still trades") {
            battlefield.contains(other) shouldBe false
            battlefield.contains(otherBlocker) shouldBe false
        }
    }
})
