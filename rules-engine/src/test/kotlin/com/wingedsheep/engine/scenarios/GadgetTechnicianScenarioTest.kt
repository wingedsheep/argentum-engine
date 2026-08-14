package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.GadgetTechnician
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gadget Technician (MKM #204) — {2}{U}{R} 3/2 Goblin Artificer, Disguise {U/R}{U/R}.
 *
 * "When this creature enters **or is turned face up**, create a 1/1 colorless Thopter artifact
 *  creature token with flying."
 *
 * The whole point of the test is that both halves of the disjunctive trigger fire — the shape a
 * naive `Triggers.EntersBattlefield` would silently half-implement, since a card cast face down and
 * flipped never enters again (CR 701.34) and would produce no Thopter at all.
 *
 * The third test pins the other direction: a *face-down* Gadget Technician has no abilities
 * (CR 708.2), so entering the battlefield face down must not make a Thopter either. Together they
 * fence the trigger to exactly the two events the card prints.
 */
class GadgetTechnicianScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GadgetTechnician)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun thopters(driver: GameTestDriver, player: EntityId): Int =
        driver.getPermanents(player).count {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Thopter"
        }

    /** Cast it face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Gadget Technician")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
        return driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
    }

    test("hard-casting it makes a Thopter — the enters half of the trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val card = driver.putCardInHand(player, "Gadget Technician")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        driver.findPermanent(player, "Gadget Technician").shouldNotBeNull()
        withClue("the enters trigger resolved into exactly one Thopter") {
            thopters(driver, player) shouldBe 1
        }
    }

    test("turning it face up makes a Thopter — the flip half of the trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val technician = castFaceDown(driver, player)
        withClue("a face-down permanent has no abilities (CR 708.2), so entering made no Thopter") {
            thopters(driver, player) shouldBe 0
        }

        // Disguise {U/R}{U/R} — two blue pays both hybrid pips.
        driver.giveMana(player, Color.BLUE, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = technician,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("turning face up is not entering (CR 701.34) — only the second half can have fired") {
            thopters(driver, player) shouldBe 1
        }
        driver.state.getEntity(technician)?.has<FaceDownComponent>() shouldBe false
    }

    test("the hybrid disguise cost is payable with either color alone") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val technician = castFaceDown(driver, player)

        driver.giveMana(player, Color.RED, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = technician,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("{U/R}{U/R} takes two red just as happily as two blue") {
            driver.state.getEntity(technician)?.has<FaceDownComponent>() shouldBe false
            thopters(driver, player) shouldBe 1
        }
    }
})
