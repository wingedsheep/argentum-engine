package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Clone.
 *
 * Clone: {3}{U}
 * Creature — Shapeshifter
 * 0/0
 * You may have Clone enter the battlefield as a copy of any creature on the battlefield.
 */
class CloneTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Clone copies a creature on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield to copy
        val warrior = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")

        // Cast Clone
        val clone = driver.putCardInHand(activePlayer, "Clone")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, clone)

        // Both pass priority to resolve
        driver.bothPass()

        // Should have a SelectCardsDecision for choosing creature to copy
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.minSelections shouldBe 0  // optional
        decision.maxSelections shouldBe 1
        decision.options shouldContain warrior

        // Select the warrior to copy
        driver.submitCardSelection(activePlayer, listOf(warrior))

        // Clone should now be a 2/3 Elvish Warrior
        val cloneCard = driver.state.getEntity(clone)?.get<CardComponent>()
        cloneCard shouldNotBe null
        cloneCard!!.name shouldBe "Elvish Warrior"
        projector.getProjectedPower(driver.state, clone) shouldBe 2
        projector.getProjectedToughness(driver.state, clone) shouldBe 3

        // Should have CopyOfComponent tracking
        val copyOf = driver.state.getEntity(clone)?.get<CopyOfComponent>()
        copyOf shouldNotBe null
        copyOf!!.originalCardDefinitionId shouldBe "Clone"
        copyOf.copiedCardDefinitionId shouldBe "Elvish Warrior"

        // Should be on the battlefield
        driver.state.getBattlefield() shouldContain clone
    }

    test("Clone enters as 0/0 when player declines to copy") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")

        // Cast Clone
        val clone = driver.putCardInHand(activePlayer, "Clone")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, clone)
        driver.bothPass()

        // Decline to copy (select nothing)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(activePlayer, emptyList())

        // Clone enters as 0/0 Shapeshifter and should die to SBAs
        val onBattlefield = driver.state.getBattlefield().contains(clone)
        onBattlefield shouldBe false
    }

    test("Clone enters as 0/0 when no creatures exist on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No creatures on battlefield - cast Clone directly
        val clone = driver.putCardInHand(activePlayer, "Clone")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, clone)
        driver.bothPass()

        // No decision should be presented when no creatures exist
        // Clone enters as 0/0 and dies to SBAs
        val onBattlefield = driver.state.getBattlefield().contains(clone)
        onBattlefield shouldBe false
    }

    test("Clone copies a creature with keywords") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a flying creature on the battlefield
        val drake = driver.putCreatureOnBattlefield(activePlayer, "Wind Drake")

        // Cast Clone
        val clone = driver.putCardInHand(activePlayer, "Clone")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, clone)
        driver.bothPass()

        // Select the drake to copy
        driver.submitCardSelection(activePlayer, listOf(drake))

        // Clone should be a 2/2 with flying
        projector.getProjectedPower(driver.state, clone) shouldBe 2
        projector.getProjectedToughness(driver.state, clone) shouldBe 2

        val projected = projector.project(driver.state)
        projected.hasKeyword(clone, Keyword.FLYING.name) shouldBe true
    }

    test("Clone can copy opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != activePlayer }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a creature
        val giant = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        // Cast Clone
        val clone = driver.putCardInHand(activePlayer, "Clone")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, clone)
        driver.bothPass()

        // Select opponent's creature
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options shouldContain giant
        driver.submitCardSelection(activePlayer, listOf(giant))

        // Clone should be a 3/3 Hill Giant
        projector.getProjectedPower(driver.state, clone) shouldBe 3
        projector.getProjectedToughness(driver.state, clone) shouldBe 3

        val cloneCard = driver.state.getEntity(clone)?.get<CardComponent>()
        cloneCard!!.name shouldBe "Hill Giant"

        // Clone should still be controlled by active player
        driver.state.getBattlefield() shouldContain clone
    }
})
