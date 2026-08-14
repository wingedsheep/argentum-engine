package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.TheSoulStone
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Soul Stone (SPM) — "{6}{B}, {T}, Exile a creature you control: Harness The Soul Stone. Once
 * harnessed, its ∞ ability is active. ∞ — At the beginning of your upkeep, return target creature
 * card from your graveyard to the battlefield."
 *
 * Pins the Harness gating: the ∞ upkeep trigger reanimates only while the Stone has a harness
 * counter (the new `Counters.HARNESS` marker read by `Conditions.SourceHasCounter`).
 */
class TheSoulStoneScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheSoulStone))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return driver to you
    }

    /** Advance to the controller's own next upkeep, submitting the ∞ target if it triggers. */
    fun advanceToMyUpkeep(driver: GameTestDriver, me: EntityId, reanimateTarget: EntityId?) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != me && !driver.isPaused) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        // Resolve the upkeep: submit the ∞ target when prompted, otherwise pass through the stack.
        var guard = 0
        while (guard++ < 60) {
            if (driver.isPaused && driver.pendingDecision is ChooseTargetsDecision) {
                if (reanimateTarget == null) break
                driver.submitTargetSelection(me, listOf(reanimateTarget))
            } else if (!driver.isPaused && driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else {
                break
            }
        }
    }

    test("once harnessed, the ∞ upkeep trigger reanimates a creature from your graveyard") {
        val (driver, you) = newGame()
        val stone = driver.putPermanentOnBattlefield(you, "The Soul Stone")
        driver.addComponent(stone, CountersComponent(mapOf(CounterType.HARNESS to 1)))
        val lions = driver.putCardInGraveyard(you, "Savannah Lions")

        advanceToMyUpkeep(driver, you, reanimateTarget = lions)

        driver.getPermanents(you).contains(lions) shouldBe true
        driver.getGraveyard(you).contains(lions) shouldBe false
    }

    test("without a harness counter the ∞ ability does not trigger") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "The Soul Stone") // NOT harnessed
        val lions = driver.putCardInGraveyard(you, "Savannah Lions")

        advanceToMyUpkeep(driver, you, reanimateTarget = null)

        driver.getGraveyard(you).contains(lions) shouldBe true
        driver.getPermanents(you).contains(lions) shouldBe false
    }
})
