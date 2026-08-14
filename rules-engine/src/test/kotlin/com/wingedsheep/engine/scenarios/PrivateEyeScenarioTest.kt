package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NoviceInspector
import com.wingedsheep.mtg.sets.definitions.mkm.cards.PrivateEye
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Private Eye (MKM #223) — {1}{W}{U} 3/3 Homunculus Detective.
 *
 * "Other Detectives you control get +1/+1.
 *  Whenever you draw your second card each turn, target Detective can't be blocked this turn."
 *
 * Two claims, one per ability. The lord must be **other**-scoped — Private Eye is itself a Detective,
 * so a missing `excludeSelf` would quietly make it a 4/4. And the payoff must key off the *second*
 * card drawn each turn (CR 121.2), not off any draw, so the first draw of the turn has to be inert.
 *
 * Draws are driven by a free instant so the only variable under test is the draw count.
 */
class PrivateEyeScenarioTest : FunSpec({

    val projector = StateProjector()

    // A free instant: the cast costs nothing, so nothing but the draw count moves.
    val drawOne = card("Private Eye Draw One Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(PrivateEye, NoviceInspector, drawOne))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun drawACard(driver: GameTestDriver, player: EntityId) {
        val card = driver.putCardInHand(player, "Private Eye Draw One Test")
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()
    }

    test("the lord pumps other Detectives but never itself") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val eye = driver.putCreatureOnBattlefield(player, "Private Eye")
        val inspector = driver.putCreatureOnBattlefield(player, "Novice Inspector")
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val projected = projector.project(driver.state)
        withClue("Novice Inspector is a 1/2 Human Detective, so it gets +1/+1") {
            projected.getPower(inspector) shouldBe 2
            projected.getToughness(inspector) shouldBe 3
        }
        withClue("\"other\" excludes the Eye itself — it stays a printed 3/3") {
            projected.getPower(eye) shouldBe 3
            projected.getToughness(eye) shouldBe 3
        }
        withClue("a non-Detective is untouched") {
            projected.getPower(courser) shouldBe 3
            projected.getToughness(courser) shouldBe 3
        }
    }

    test("the second draw of the turn makes a Detective unblockable; the first does not") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Private Eye")
        val inspector = driver.putCreatureOnBattlefield(player, "Novice Inspector")

        drawACard(driver, player)
        withClue("one draw doesn't reach NthCardDrawn(2)") {
            driver.stackSize shouldBe 0
            projector.project(driver.state)
                .hasKeyword(inspector, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
        }

        drawACard(driver, player)

        // The trigger targets a Detective; with two on the battlefield the choice is a real decision.
        (driver.pendingDecision as? ChooseTargetsDecision)?.let {
            driver.submitTargetSelection(player, listOf(inspector))
        }
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("the second draw fired the trigger and the chosen Detective is unblockable") {
            projector.project(driver.state)
                .hasKeyword(inspector, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }
    }

    test("the grant is until end of turn only") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Private Eye")
        val inspector = driver.putCreatureOnBattlefield(player, "Novice Inspector")

        drawACard(driver, player)
        drawACard(driver, player)
        (driver.pendingDecision as? ChooseTargetsDecision)?.let {
            driver.submitTargetSelection(player, listOf(inspector))
        }
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
        projector.project(driver.state)
            .hasKeyword(inspector, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        withClue("\"this turn\" expired in the cleanup step") {
            projector.project(driver.state)
                .hasKeyword(inspector, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
        }
    }
})
