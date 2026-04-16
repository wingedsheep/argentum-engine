package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastModalModeSelectionContinuation
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests I1 / I2 / I3 from [`backlog/modal-cast-time-choices-plan.md`]: the
 * `minChooseCount < chooseCount` case (rule 700.2 — "choose one or both").
 *
 * The UI contract is driven entirely through [ChooseOptionDecision] options:
 *   - A trailing "Done" option becomes available once
 *     `selectedModeIndices.size >= minChooseCount`, letting the player finalize
 *     early without picking every mode.
 *   - "Done" must NOT appear before `minChooseCount` picks.
 *
 * Synthetic test card: `modal(chooseCount = 2, minChooseCount = 1)`, two modes
 * ("Draw a card", "Gain 3 life"). Functionally an Austere-Command-lite.
 */
class ModalMinChooseCountTest : FunSpec({

    val ChooseOneOrBoth: CardDefinition = card("Test One Or Both") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            modal(chooseCount = 2, minChooseCount = 1) {
                mode("Draw a card", Effects.DrawCards(1))
                mode("Gain 3 life", Effects.GainLife(3))
            }
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ChooseOneOrBoth))
        return d
    }

    test("I1 — pick one mode then Done; only that mode resolves") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val handSizeBefore = d.state.getHand(p1).size
        val lifeBefore = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life

        val spell = d.putCardInHand(p1, "Test One Or Both")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        // First pick (none selected yet) — Done MUST NOT be offered: selected.size (0) is
        // less than minChooseCount (1).
        val first = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        first.options shouldContain "Draw a card"
        first.options shouldContain "Gain 3 life"
        first.options shouldNotContain "Done"
        first.options shouldHaveSize 2

        // Pick mode 0 ("Draw a card").
        d.submitDecision(p1, OptionChosenResponse(first.id, 0))

        // Second decision — one mode selected, chooseCount=2, so Done IS offered (per rule:
        // size >= minChooseCount AND size < chooseCount).
        val second = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        second.options shouldContain "Done"
        // Remaining narrowed options (allowRepeat=false): "Gain 3 life".
        second.options shouldContain "Gain 3 life"
        second.options shouldNotContain "Draw a card"

        // Pick "Done" — its offered-position is the tail slot, equal to the number of
        // real mode options on this step (one remaining mode + Done).
        val doneIndex = second.options.indexOf("Done")
        d.submitDecision(p1, OptionChosenResponse(second.id, doneIndex))

        // Spell resolves with chosenModes = [0]: draw 1 card, no life gain.
        d.bothPass()
        val handSizeAfter = d.state.getHand(p1).size
        val lifeAfter = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life

        // Hand: handSizeBefore + 1 putCardInHand - 1 cast + 1 draw = handSizeBefore + 1.
        handSizeAfter shouldBe (handSizeBefore + 1)
        lifeAfter shouldBe lifeBefore  // Gain-3-life mode was not picked.
    }

    test("I2 — pick both modes; both resolve") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val handSizeBefore = d.state.getHand(p1).size
        val lifeBefore = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life

        val spell = d.putCardInHand(p1, "Test One Or Both")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(p1, OptionChosenResponse(first.id, 0))  // Draw a card

        val second = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Pick the remaining real mode (not Done).
        val gainIndex = second.options.indexOf("Gain 3 life")
        d.submitDecision(p1, OptionChosenResponse(second.id, gainIndex))

        // Both modes resolve: draw 1 + gain 3 life.
        d.bothPass()
        val handSizeAfter = d.state.getHand(p1).size
        val lifeAfter = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life

        handSizeAfter shouldBe (handSizeBefore + 1)
        lifeAfter shouldBe (lifeBefore + 3)
    }

    test("I3 — 'Done' not offered before minChooseCount is reached") {
        // Regression: Done must NOT appear on the first decision. If it did, the player
        // could cast a modal spell and pick zero modes — a rules violation.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val spell = d.putCardInHand(p1, "Test One Or Both")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        first.options shouldNotContain "Done"

        // Continuation state confirms doneOptionOffered=false and selectedModeIndices empty.
        val continuation = d.state.continuationStack
            .filterIsInstance<CastModalModeSelectionContinuation>()
            .single()
        continuation.doneOptionOffered shouldBe false
        continuation.selectedModeIndices shouldBe emptyList()
        continuation.minChooseCount shouldBe 1
        continuation.chooseCount shouldBe 2
    }
})
