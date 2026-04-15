package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.ManifoldMouse
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Manifold Mouse (BLB).
 *
 * Regression guard for the "target + ModalEffect with Mode.noTarget" pattern:
 * the BeginCombat trigger targets a Mouse, then the controller chooses double
 * strike or trample. The inner GrantKeywordEffect uses `ContextTarget(0)` to
 * refer to the targeted Mouse. Previously, ModalEffectExecutor dropped the
 * outer targets when building its continuation, so no-target modes ran with an
 * empty target list and the keyword was never granted.
 */
class ManifoldMouseTest : FunSpec({

    // Minimal Mouse creature for targeting — avoids depending on set-specific Mouse cards.
    val TestMouse = CardDefinition.creature(
        name = "Test Mouse",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Mouse")),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ManifoldMouse, TestMouse))
        return driver
    }

    // Player 1 may not be active at game start (random turn order) — advance until it is.
    fun GameTestDriver.advanceToPlayer1BeginCombat() {
        passPriorityUntil(Step.BEGIN_COMBAT)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.BEGIN_COMBAT)
            safety++
        }
    }

    test("choosing Double strike grants double strike to the targeted Mouse") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val mouse = driver.putCreatureOnBattlefield(driver.player1, "Test Mouse")
        driver.putCreatureOnBattlefield(driver.player1, "Manifold Mouse")
        driver.removeSummoningSickness(mouse)

        driver.advanceToPlayer1BeginCombat()

        // BeginCombat trigger fires; first decision is target selection for the trigger.
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(mouse))

        // Both pass → trigger resolves → ModalEffectExecutor presents mode choice.
        driver.bothPass()

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        modeDecision.options shouldBe listOf("Double strike", "Trample")
        driver.submitDecision(driver.player1, OptionChosenResponse(modeDecision.id, 0))

        val projected = projector.project(driver.state)
        projected.hasKeyword(mouse, Keyword.DOUBLE_STRIKE) shouldBe true
        projected.hasKeyword(mouse, Keyword.TRAMPLE) shouldBe false
    }

    test("choosing Trample grants trample to the targeted Mouse") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val mouse = driver.putCreatureOnBattlefield(driver.player1, "Test Mouse")
        driver.putCreatureOnBattlefield(driver.player1, "Manifold Mouse")
        driver.removeSummoningSickness(mouse)

        driver.advanceToPlayer1BeginCombat()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(mouse))
        driver.bothPass()

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(driver.player1, OptionChosenResponse(modeDecision.id, 1))

        val projected = projector.project(driver.state)
        projected.hasKeyword(mouse, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(mouse, Keyword.DOUBLE_STRIKE) shouldBe false
    }

    test("granted double strike deals damage in both first strike and regular combat damage steps") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val mouse = driver.putCreatureOnBattlefield(driver.player1, "Test Mouse")
        driver.putCreatureOnBattlefield(driver.player1, "Manifold Mouse")
        driver.removeSummoningSickness(mouse)

        driver.advanceToPlayer1BeginCombat()

        // Resolve the BeginCombat trigger picking Double strike for the mouse.
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(driver.player1, listOf(mouse))
        driver.bothPass()
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(driver.player1, OptionChosenResponse(modeDecision.id, 0))

        // Advance to declare attackers and swing with the Mouse.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(mouse), driver.player2)

        // Unblocked double-strike attacker deals 1 damage twice → opponent loses 2 life.
        driver.bothPass() // end declare attackers
        driver.bothPass() // end declare blockers (no blockers)
        driver.bothPass() // first-strike damage step (1 damage)
        driver.bothPass() // regular combat damage step (another 1 damage)

        driver.assertLifeTotal(driver.player2, 18)
    }
})
