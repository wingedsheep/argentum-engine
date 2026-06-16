package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ClaimJumper
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Claim Jumper — {2}{W} Creature — Rabbit Mercenary 3/3, Vigilance.
 *
 * "When this creature enters, if an opponent controls more lands than you, you may search your
 *  library for a Plains card and put it onto the battlefield tapped. Then if an opponent controls
 *  more lands than you, repeat this process once. If you search your library this way, shuffle."
 */
class OtjClaimJumperScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ClaimJumper)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolve one "may search" process step: answer the may, then select a Plains if offered. */
    fun GameTestDriver.resolveMaySearch(player: EntityId, accept: Boolean) {
        val may = pendingDecision
        (may is YesNoDecision) shouldBe true
        submitYesNo(player, accept)
        if (accept) {
            val search = pendingDecision
            (search is SelectCardsDecision) shouldBe true
            search as SelectCardsDecision
            submitCardSelection(player, listOf(search.options.first()))
        }
    }

    test("when an opponent controls more lands, may fetch up to two tapped Plains (process repeats once)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Opponent controls more lands than me: 0 vs 3.
        repeat(3) { driver.putLandOnBattlefield(opp, "Plains") }

        val landsBefore = driver.getLands(me).size

        val jumper = driver.putCardInHand(me, "Claim Jumper")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(com.wingedsheep.engine.core.CastSpell(playerId = me, cardId = jumper))
        driver.bothPass() // resolve Jumper onto battlefield
        driver.bothPass() // begin resolving the ETB trigger -> first may-search

        driver.resolveMaySearch(me, accept = true)  // first Plains, tapped
        driver.resolveMaySearch(me, accept = true)  // still behind -> repeat once

        val newLands = driver.getLands(me).filter { driver.getCardName(it) == "Plains" }
        driver.getLands(me).size shouldBe landsBefore + 2
        // Fetched lands enter tapped.
        newLands.count { driver.isTapped(it) } shouldBe 2
    }

    test("no trigger ability does anything when you are not behind on lands") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // I control more lands than the opponent: 2 vs 0. Intervening-if fails, no search.
        repeat(2) { driver.putLandOnBattlefield(me, "Plains") }
        val landsBefore = driver.getLands(me).size

        val jumper = driver.putCardInHand(me, "Claim Jumper")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(com.wingedsheep.engine.core.CastSpell(playerId = me, cardId = jumper))
        driver.bothPass() // resolve onto battlefield
        driver.bothPass() // ETB trigger should do nothing (intervening-if fails)

        // Jumper entered, but the intervening-if failed: no may decision, no land fetched.
        driver.findPermanent(me, "Claim Jumper") shouldNotBe null
        (driver.pendingDecision is YesNoDecision) shouldBe false
        driver.getLands(me).size shouldBe landsBefore
    }

    test("declining the may search performs no search and skips the repeat") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        repeat(3) { driver.putLandOnBattlefield(opp, "Plains") }
        val landsBefore = driver.getLands(me).size

        val jumper = driver.putCardInHand(me, "Claim Jumper")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(com.wingedsheep.engine.core.CastSpell(playerId = me, cardId = jumper))
        driver.bothPass()
        driver.bothPass()

        // Decline the first may. Still behind on lands, so the process repeats; decline again.
        driver.resolveMaySearch(me, accept = false)
        driver.resolveMaySearch(me, accept = false)

        driver.getLands(me).size shouldBe landsBefore
    }
})
