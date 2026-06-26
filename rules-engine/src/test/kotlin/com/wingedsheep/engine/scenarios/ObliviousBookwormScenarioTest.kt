package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Oblivious Bookworm {G}{U} 2/3 Human Wizard.
 *
 * "At the beginning of your end step, you may draw a card. If you do, discard a card unless a
 * permanent entered the battlefield face down under your control this turn or you turned a
 * permanent face up this turn."
 *
 * Exercises the two new per-player turn trackers
 * ([com.wingedsheep.sdk.scripting.values.TurnTracker.PERMANENTS_ENTERED_FACE_DOWN] /
 * [com.wingedsheep.sdk.scripting.values.TurnTracker.PERMANENTS_TURNED_FACE_UP]) end to end — both
 * the write paths (real `TurnFaceUp` action; real manifest ETB) and the read path (the gated
 * discard) — plus their end-of-turn reset.
 */
class ObliviousBookwormScenarioTest : FunSpec({

    // A bare {G} sorcery that manifests the top card face down — a real face-down ETB through the
    // sanctioned battlefield-entry pipeline (so PermanentEntryTracker.record runs).
    val manifestTester = card("Bookworm Manifest Tester") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.manifest() }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + manifestTester)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30, "Island" to 30), startingLife = 20)
        return driver
    }

    // Put a face-down morph creature directly onto the battlefield (mirrors MorphCostPaymentTest).
    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = TestCards.all.first { it.name == cardName }
        val morph = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morph != null) c = c.with(MorphDataComponent(morph.morphCost, cardDef.name))
            c
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    /** Advance to [player]'s end step and resolve the Bookworm trigger up to the may-draw yes/no. */
    fun GameTestDriver.reachBookwormMayDraw(player: EntityId) {
        passPriorityUntil(Step.END)
        var guard = 0
        while (pendingDecision == null && guard++ < 6) bothPass()
    }

    test("no face-down activity this turn: draw then discard (net hand size unchanged)") {
        val driver = createDriver()
        val player = driver.player1
        driver.putCreatureOnBattlefield(player, "Oblivious Bookworm")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHandSize(player)

        driver.reachBookwormMayDraw(player)
        driver.submitYesNo(player, true) // may draw — yes

        // Neither face-down fact holds, so the unless-gate is open: a discard is forced.
        driver.getHandSize(player) shouldBe handBefore + 1 // drew, discard not yet chosen
        val toDiscard = driver.getHand(player).first()
        driver.submitCardSelection(player, listOf(toDiscard))

        driver.getHandSize(player) shouldBe handBefore // drew 1, discarded 1
    }

    test("declining the may draws nothing and discards nothing") {
        val driver = createDriver()
        val player = driver.player1
        driver.putCreatureOnBattlefield(player, "Oblivious Bookworm")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHandSize(player)

        driver.reachBookwormMayDraw(player)
        driver.submitYesNo(player, false) // decline

        driver.getHandSize(player) shouldBe handBefore
        driver.pendingDecision.shouldBeNull() // no discard selection
    }

    test("turned a permanent face up this turn: draw, no discard") {
        val driver = createDriver()
        val player = driver.player1
        driver.putCreatureOnBattlefield(player, "Oblivious Bookworm")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Real turn-face-up: Zombie Cutthroat's morph is "pay 5 life" (a non-mana cost that flips
        // via TurnFaceUpExecutor, so FaceUpTracker.record runs).
        val cutthroat = driver.putFaceDownCreature(player, "Zombie Cutthroat")
        driver.submit(TurnFaceUp(playerId = player, sourceId = cutthroat)).error shouldBe null
        driver.submitYesNo(player, true) // pay the 5 life
        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>().shouldBeNull()

        val handBefore = driver.getHandSize(player)
        driver.reachBookwormMayDraw(player)
        driver.submitYesNo(player, true) // may draw — yes

        // Gate closed by "you turned a permanent face up this turn": draw, no discard.
        driver.getHandSize(player) shouldBe handBefore + 1
        driver.pendingDecision.shouldBeNull()
    }

    test("a permanent entered the battlefield face down this turn: draw, no discard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Oblivious Bookworm")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Real face-down ETB: manifest the top card via the {G} tester sorcery.
        driver.putCardOnTopOfLibrary(player, "Centaur Courser")
        val spell = driver.putCardInHand(player, "Bookworm Manifest Tester")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, spell)
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        val handBefore = driver.getHandSize(player)
        driver.reachBookwormMayDraw(player)
        driver.submitYesNo(player, true) // may draw — yes

        // Gate closed by "a permanent entered face down under your control this turn".
        driver.getHandSize(player) shouldBe handBefore + 1
        driver.pendingDecision.shouldBeNull()
    }

    test("the trackers reset at end of turn: gate is open again on the next end step") {
        val driver = createDriver()
        val player = driver.player1
        driver.putCreatureOnBattlefield(player, "Oblivious Bookworm")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Turn 1: turn a permanent face up, so the turn-1 end step skips the discard.
        val cutthroat = driver.putFaceDownCreature(player, "Zombie Cutthroat")
        driver.submit(TurnFaceUp(playerId = player, sourceId = cutthroat)).error shouldBe null
        driver.submitYesNo(player, true)

        var handBefore = driver.getHandSize(player)
        driver.reachBookwormMayDraw(player)
        driver.submitYesNo(player, true)
        driver.getHandSize(player) shouldBe handBefore + 1 // no discard turn 1
        driver.pendingDecision.shouldBeNull()

        // Advance to player1's NEXT turn's end step (no face-down activity this time).
        // passPriorityUntil auto-resolves the incidental cleanup discard-to-hand-size and stops at
        // the step boundary before the Bookworm trigger resolves, so the may-draw isn't auto-answered.
        driver.passPriorityUntil(Step.UPKEEP) // -> opponent's turn (UNTAP is auto-skipped)
        driver.passPriorityUntil(Step.END)    // -> opponent's end step
        driver.passPriorityUntil(Step.UPKEEP) // -> player1's next turn
        driver.passPriorityUntil(Step.END)    // -> player1's next end step
        driver.activePlayer shouldBe player

        // Resolve the next end-step trigger up to the may-draw.
        var guard = 0
        while (driver.pendingDecision == null && guard++ < 6) driver.bothPass()
        handBefore = driver.getHandSize(player)
        driver.submitYesNo(player, true) // draw

        // Tracker was reset by cleanup: the gate is open again, so a discard is forced.
        driver.getHandSize(player) shouldBe handBefore + 1
        val toDiscard = driver.getHand(player).first()
        driver.submitCardSelection(player, listOf(toDiscard))
        driver.getHandSize(player) shouldBe handBefore
    }
})
