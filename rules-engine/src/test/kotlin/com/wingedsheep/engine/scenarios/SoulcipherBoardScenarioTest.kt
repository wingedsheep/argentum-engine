package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.SoulcipherBoard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Soulcipher Board // Cipherbound Spirit (VOW) — {1}{U} Artifact.
 *
 * "This artifact enters with three omen counters on it."
 * "{1}{U}, {T}: Look at the top two cards of your library. Put one of them into your graveyard."
 * "Whenever a creature card is put into your graveyard from anywhere, remove an omen counter from
 *  this artifact. Then if it has no omen counters on it, transform it."
 *
 * Covers the new [com.wingedsheep.sdk.core.Counters.OMEN] countdown counter, the per-card (not
 * batching) graveyard trigger, and the transform once the last counter is gone.
 */
class SoulcipherBoardScenarioTest : FunSpec({

    fun omenCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OMEN) ?: 0

    /** Cast Soulcipher Board for real so its enters-with-counters replacement runs. */
    fun boardOnBattlefield(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SoulcipherBoard)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val card = driver.putCardInHand(me, "Soulcipher Board")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, card).isSuccess shouldBe true
        driver.bothPass()

        return driver to driver.findPermanent(me, "Soulcipher Board")!!
    }

    test("enters with three omen counters") {
        val (driver, board) = boardOnBattlefield()
        omenCounters(driver, board) shouldBe 3
    }

    test("the tap ability puts one of the top two cards into the graveyard and leaves the other on the library") {
        val (driver, board) = boardOnBattlefield()
        val me = driver.activePlayer!!

        // Stack the library so the top two are known: Grizzly Bears on top, Lightning Bolt beneath.
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLUE, 1)
        val activation = driver.legalActions(me).first { it.description.contains("Look at the top two") }
        driver.submitSuccess(activation.action)
        driver.bothPass()

        // Choose the Bears to go to the graveyard.
        driver.submitCardSelection(me, listOf(bears)).isSuccess shouldBe true
        driver.bothPass()

        withClue("the chosen card is put into the graveyard") {
            driver.getGraveyard(me).contains(bears) shouldBe true
        }
        withClue("the other card stays in the library, on top") {
            driver.getGraveyard(me).contains(bolt) shouldBe false
        }
        withClue("a creature card hit the graveyard, so one omen counter came off") {
            omenCounters(driver, board) shouldBe 2
        }
    }

    test("three creature cards in the graveyard transform it into Cipherbound Spirit") {
        val (driver, board) = boardOnBattlefield()
        val me = driver.activePlayer!!

        repeat(3) { i ->
            val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val bolt = driver.putCardInHand(me, "Lightning Bolt")
            driver.giveMana(me, Color.RED, 1)
            driver.castSpell(me, bolt, listOf(bears)).isSuccess shouldBe true
            driver.bothPass() // resolve the Bolt; the Bears dies
            driver.bothPass() // resolve the omen-counter trigger

            if (i < 2) {
                withClue("after ${i + 1} creature death(s) the board still has counters") {
                    omenCounters(driver, board) shouldBe 2 - i
                    driver.findPermanent(me, "Soulcipher Board") shouldNotBe null
                }
            }
        }

        withClue("the last omen counter coming off transforms the artifact") {
            driver.findPermanent(me, "Cipherbound Spirit") shouldNotBe null
            driver.getCardName(board) shouldBe "Cipherbound Spirit"
        }
    }

    test("a noncreature card reaching the graveyard removes no counter") {
        val (driver, board) = boardOnBattlefield()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // The Bolt itself is an instant card put into my graveyard as it resolves.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opp)).isSuccess shouldBe true
        driver.bothPass()

        driver.getGraveyard(me).contains(bolt) shouldBe true
        withClue("only creature cards count down the omen counters") {
            omenCounters(driver, board) shouldBe 3
        }
    }
})
