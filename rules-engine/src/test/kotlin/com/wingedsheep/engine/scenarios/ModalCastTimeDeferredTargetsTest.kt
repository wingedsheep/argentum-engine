package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TrashTheTown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Covers the single-panel client mode-selection path for choose-N modal (Spree) spells:
 * the client picks the mode subset locally (from the `modalEnumeration` legal-action
 * payload) and submits a [CastSpell] with `chosenModes` populated but **targets deferred**
 * (no `targets` / `modeTargetsOrdered`). The engine must then drive the existing
 * on-battlefield per-mode target selection rather than rejecting the cast or fizzling.
 *
 * This is the server-side counterpart to the client `modalModes` pipeline phase. The other
 * Spree paths (modes + targets supplied up front) are exercised in [TrashTheTownScenarioTest].
 */
class ModalCastTimeDeferredTargetsTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TrashTheTown)
        return driver
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("single mode chosen with targets deferred → engine pauses for the mode's target, then resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2
        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 2)      // {2} for mode 0

        // Client mode selector submits the chosen mode only — no targets yet.
        val result = driver.submit(
            CastSpell(playerId = player, cardId = spell, chosenModes = listOf(0))
        )
        // The cast is accepted but pauses for targets rather than being rejected/fizzling.
        result.error shouldBe null
        result.isPaused shouldBe true

        // Engine should pause for the chosen mode's target (rule 601.2c), not reject the cast.
        val decision = driver.pendingDecision as? ChooseTargetsDecision
            ?: error("Expected a per-mode ChooseTargetsDecision, got ${driver.pendingDecision}")
        decision.playerId shouldBe player

        driver.submitTargetSelection(player, listOf(bear))
        driver.bothPass()

        driver.plusCounters(bear) shouldBe 2
    }

    test("two modes chosen with targets deferred → engine collects a target per mode, both resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 3)      // {2} + {1} for modes 0 and 1

        val result = driver.submit(
            CastSpell(playerId = player, cardId = spell, chosenModes = listOf(0, 1))
        )
        result.error shouldBe null
        result.isPaused shouldBe true

        // First mode's target.
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(player, listOf(bear))

        // Second mode's target — a fresh decision for the next chosen mode.
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(player, listOf(bear))

        driver.bothPass()

        driver.plusCounters(bear) shouldBe 2
        driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
    }
})
