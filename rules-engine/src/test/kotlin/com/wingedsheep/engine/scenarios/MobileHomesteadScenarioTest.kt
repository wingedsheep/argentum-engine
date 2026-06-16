package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.MobileHomestead
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mobile Homestead — {2} Artifact — Vehicle 3/3
 *
 * "This Vehicle has haste as long as you control a Mount.
 *  Whenever this Vehicle attacks, look at the top card of your library. If it's a land
 *  card, you may put it onto the battlefield tapped.
 *  Crew 2"
 *
 * Verifies the conditional haste static (granted only while a Mount is controlled) and the
 * attack trigger that puts a land from the top of the library onto the battlefield tapped —
 * and that a non-land top card is left untouched.
 */
class MobileHomesteadScenarioTest : FunSpec({

    val projector = StateProjector()

    // A vanilla Mount to satisfy "as long as you control a Mount".
    val testMount = card("Test Mount") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Horse Mount"
        power = 2
        toughness = 2
        oracleText = ""
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(testMount)
        // Forest-heavy deck; we control the top card explicitly in each test.
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("has haste only while you control a Mount") {
        val driver = createDriver()
        val me = driver.player1

        val homestead = driver.putPermanentOnBattlefield(me, "Mobile Homestead")

        // No Mount yet → no haste.
        projector.project(driver.state).hasKeyword(homestead, Keyword.HASTE) shouldBe false

        // Control a Mount → haste.
        driver.putCreatureOnBattlefield(me, "Test Mount")
        projector.project(driver.state).hasKeyword(homestead, Keyword.HASTE) shouldBe true
    }

    test("attack trigger puts a land from the top of the library onto the battlefield tapped") {
        val driver = createDriver()
        val me = driver.player1

        val homestead = driver.putPermanentOnBattlefield(me, "Mobile Homestead")
        driver.removeSummoningSickness(homestead)

        // Crew it so it can attack (Crew 2: a power-2 creature).
        val crewer = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        driver.submitSuccess(CrewVehicle(me, homestead, listOf(crewer)))
        driver.bothPass()

        // Put a land on top of the library.
        driver.putCardOnTopOfLibrary(me, "Mountain")
        val landsBefore = driver.getPermanents(me).count { driver.getCardName(it) == "Mountain" }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(homestead), driver.player2)
        // Let the attack trigger go on the stack and resolve, which pauses for the selection.
        driver.bothPass()

        // Attack trigger offers to put the land onto the battlefield — accept it.
        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitCardSelection(me, decision.options)
        driver.bothPass()

        val mountainPerms = driver.getPermanents(me).filter { driver.getCardName(it) == "Mountain" }
        mountainPerms.size shouldBe landsBefore + 1
        // The single new land entered tapped.
        driver.isTapped(mountainPerms.single()) shouldBe true
    }

    test("attack trigger leaves a non-land top card on the library") {
        val driver = createDriver()
        val me = driver.player1

        val homestead = driver.putPermanentOnBattlefield(me, "Mobile Homestead")
        driver.removeSummoningSickness(homestead)

        val crewer = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.submitSuccess(CrewVehicle(me, homestead, listOf(crewer)))
        driver.bothPass()

        // Non-land on top.
        driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        val courserPermsBefore = driver.getPermanents(me).count { driver.getCardName(it) == "Centaur Courser" }
        val handBefore = driver.getHandSize(me)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(homestead), driver.player2)
        // No land → no decision; trigger resolves with nothing to do.
        driver.bothPass()

        // The non-land stayed put: not on the battlefield, not drawn to hand.
        driver.getPermanents(me).count { driver.getCardName(it) == "Centaur Courser" } shouldBe courserPermsBefore
        driver.getHandSize(me) shouldBe handBefore
    }
})
