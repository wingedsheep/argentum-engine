package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GlimpseTheCore
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Glimpse the Core (LCI #186, {1}{G} Sorcery).
 *
 * "Choose one —
 *  • Search your library for a basic Forest card, put that card onto the battlefield tapped, then shuffle.
 *  • Return target Cave card from your graveyard to the battlefield tapped."
 *
 * Tests:
 *  1. Mode 0 — library search puts a basic Forest onto the battlefield tapped and shuffles.
 *  2. Mode 1 — targeting a Cave card in your graveyard puts it onto the battlefield tapped.
 */
class GlimpseTheCoreScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GlimpseTheCore)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Drain the stack, stopping at decisions or errors. */
    fun GameTestDriver.drainStack(maxIterations: Int = 20) {
        var guard = 0
        while (state.stack.isNotEmpty() && !isPaused && guard++ < maxIterations) {
            bothPass()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test 1: Mode 0 — search library for a basic Forest, put onto battlefield tapped, shuffle.
    // ─────────────────────────────────────────────────────────────────────────────
    test("mode 0: searches library for a basic Forest and puts it onto the battlefield tapped") {
        val driver = newDriver()
        val you = driver.activePlayer!!

        val spell = driver.putCardInHand(you, "Glimpse the Core")
        // {1}{G}: two green covers both the generic and colored requirements.
        driver.giveMana(you, Color.GREEN, 2)

        val cast = driver.submit(
            CastSpell(
                playerId = you,
                cardId = spell,
                targets = emptyList(),
                chosenModes = listOf(0)
            )
        )
        withClue("Casting Glimpse the Core mode 0 should succeed") {
            cast.isSuccess shouldBe true
        }

        // Resolve until the library-search SelectCardsDecision pauses.
        driver.drainStack()

        withClue("Library search should present a SelectCardsDecision") {
            driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        }

        // Select the first offered Forest from the searchable collection.
        val decision = driver.pendingDecision as SelectCardsDecision
        withClue("Library search should offer at least one basic Forest") {
            decision.options.isEmpty() shouldBe false
        }
        val forestId = decision.options.first()
        driver.submitCardSelection(you, listOf(forestId))

        // Complete remaining effects (MoveCollection, ShuffleLibrary, EmitLibrarySearched).
        driver.drainStack()

        val forestOnBattlefield = driver.findPermanent(you, "Forest")
        withClue("Forest should be on the battlefield after mode-0 resolution") {
            forestOnBattlefield shouldNotBe null
        }
        withClue("Forest should enter the battlefield tapped (ZonePlacement.Tapped)") {
            driver.state.getEntity(forestOnBattlefield!!)?.has<TappedComponent>() shouldBe true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test 2: Mode 1 — return target Cave card from graveyard to battlefield tapped.
    // ─────────────────────────────────────────────────────────────────────────────
    test("mode 1: returns target Cave card from graveyard to battlefield tapped") {
        val driver = newDriver()
        val you = driver.activePlayer!!

        // A Cave card sitting in your graveyard — Captivating Cave (Land — Cave) from LCI.
        val cave = driver.putCardInGraveyard(you, "Captivating Cave")

        val spell = driver.putCardInHand(you, "Glimpse the Core")
        // {1}{G}: two green covers both generic and colored.
        driver.giveMana(you, Color.GREEN, 2)

        val target = ChosenTarget.Card(cave, you, Zone.GRAVEYARD)
        val cast = driver.submit(
            CastSpell(
                playerId = you,
                cardId = spell,
                targets = listOf(target),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(target))
            )
        )
        withClue("Casting Glimpse the Core mode 1 should succeed") {
            cast.isSuccess shouldBe true
        }

        // Mode 1 requires no interactive decisions — resolve the whole stack.
        driver.drainStack()

        val caveOnBattlefield = driver.findPermanent(you, "Captivating Cave")
        withClue("Captivating Cave should be on the battlefield after mode-1 resolution") {
            caveOnBattlefield shouldNotBe null
        }
        withClue("Captivating Cave should enter the battlefield tapped") {
            driver.state.getEntity(caveOnBattlefield!!)?.has<TappedComponent>() shouldBe true
        }
        withClue("Captivating Cave should no longer be in the graveyard") {
            driver.getGraveyard(you).contains(cave) shouldBe false
        }
    }
})
