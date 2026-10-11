package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.HarmonicProdigy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Harmonic Prodigy (MH2 #132) — {1}{R} Creature — Human Wizard 1/3.
 *
 *   Prowess
 *   If a triggered ability of a Shaman or another Wizard you control triggers, that ability
 *   triggers an additional time.
 *
 * The doubling filter is a heterogeneous union — `Shaman or (Wizard and not this)` — so these
 * tests pin each leg: the Prodigy's own prowess is *not* doubled (it is a Wizard, but not
 * "another" one), a second Wizard's prowess is, and a Shaman's prowess is.
 */
class HarmonicProdigyScenarioTest : FunSpec({

    val prowessShaman = card("Test Prowess Shaman") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin Shaman"
        power = 1
        toughness = 1
        prowess()
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HarmonicProdigy)
        driver.registerCard(prowessShaman)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Lightning Bolt at the opponent and resolve everything, keeping trigger order as offered. */
    fun GameTestDriver.boltOpponentAndSettle(player: EntityId) {
        val bolt = putCardInHand(player, "Lightning Bolt")
        giveMana(player, Color.RED, 1)
        castSpell(player, bolt, listOf(getOpponent(player))).outcome shouldBe Outcome.Done
        repeat(20) {
            when (val d = pendingDecision) {
                is OrderObjectsDecision -> submitDecision(d.playerId, OrderedResponse(d.id, d.objects))
                null -> if (stackSize > 0) bothPass() else return
                else -> error("unexpected decision $d")
            }
        }
    }

    test("its own prowess is not doubled") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val prodigy = driver.putCreatureOnBattlefield(player, "Harmonic Prodigy")

        driver.boltOpponentAndSettle(player)

        driver.state.projectedState.getPower(prodigy) shouldBe 2
        driver.state.projectedState.getToughness(prodigy) shouldBe 4
    }

    test("another Wizard's prowess triggers an additional time") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val first = driver.putCreatureOnBattlefield(player, "Harmonic Prodigy")
        val second = driver.putCreatureOnBattlefield(player, "Harmonic Prodigy")

        driver.boltOpponentAndSettle(player)

        // Each Prodigy doubles the other's prowess: 1/3 + 2 x (+1/+1) = 3/5.
        driver.state.projectedState.getPower(first) shouldBe 3
        driver.state.projectedState.getToughness(first) shouldBe 5
        driver.state.projectedState.getPower(second) shouldBe 3
        driver.state.projectedState.getToughness(second) shouldBe 5
    }

    test("a Shaman's prowess triggers an additional time") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val prodigy = driver.putCreatureOnBattlefield(player, "Harmonic Prodigy")
        val shaman = driver.putCreatureOnBattlefield(player, "Test Prowess Shaman")

        driver.boltOpponentAndSettle(player)

        driver.state.projectedState.getPower(shaman) shouldBe 3
        driver.state.projectedState.getToughness(shaman) shouldBe 3
        driver.state.projectedState.getPower(prodigy) shouldBe 2
    }
})
