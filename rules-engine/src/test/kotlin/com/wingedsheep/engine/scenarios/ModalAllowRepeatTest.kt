package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastModalModeSelectionContinuation
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests F1 / F2 from [`backlog/modal-cast-time-choices-plan.md`]: enforcement of
 * MTG rule 700.2d — `allowRepeat` controls whether a mode can be chosen more than
 * once during cast-time mode selection.
 *
 * - F1 (`allowRepeat=false`): after a mode is picked, it must be absent from the
 *   next decision's options. This drives the narrowing logic in
 *   [CastSpellHandler.presentCastModalModeDecision] / the resumer.
 * - F2 (`allowRepeat=true`): the full offered set must remain available for the
 *   next pick, enabling Escalate / Spree–style repeated modes. The continuation's
 *   `availableIndices` stays null, and the decision's options list the same modes.
 */
class ModalAllowRepeatTest : FunSpec({

    /**
     * Synthetic choose-2 modal card with three simple, always-satisfiable modes. Two
     * variants differ only in `allowRepeat`, isolating the flag under test.
     */
    val NonRepeatModal: CardDefinition = card("Test Non-Repeat Modal") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            modal(chooseCount = 2, allowRepeat = false) {
                mode("Draw a card", Effects.DrawCards(1))
                mode("Gain 1 life", Effects.GainLife(1))
                mode("You lose 1 life", Effects.LoseLife(1))
            }
        }
    }

    val RepeatModal: CardDefinition = card("Test Repeat Modal") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            modal(chooseCount = 2, allowRepeat = true) {
                mode("Draw a card", Effects.DrawCards(1))
                mode("Gain 1 life", Effects.GainLife(1))
                mode("You lose 1 life", Effects.LoseLife(1))
            }
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NonRepeatModal, RepeatModal))
        return d
    }

    test("F1 — allowRepeat=false: mode 0 is not offered again after being picked") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val spell = d.putCardInHand(p1, "Test Non-Repeat Modal")
        val result = d.submit(CastSpell(playerId = p1, cardId = spell))
        result.isPaused shouldBe true

        // First decision offers all three modes (none picked yet).
        val firstDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        firstDecision.options shouldHaveSize 3
        firstDecision.options shouldContain "Draw a card"
        firstDecision.options shouldContain "Gain 1 life"
        firstDecision.options shouldContain "You lose 1 life"

        // Pick mode 0 ("Draw a card") at offered-position 0.
        d.submitDecision(p1, OptionChosenResponse(firstDecision.id, 0))

        val secondDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        secondDecision.options shouldHaveSize 2
        secondDecision.options shouldNotContain "Draw a card"
        secondDecision.options shouldContain "Gain 1 life"
        secondDecision.options shouldContain "You lose 1 life"

        // The continuation's narrowed availableIndices must exclude the picked mode.
        val continuation = d.state.continuationStack
            .filterIsInstance<CastModalModeSelectionContinuation>()
            .single()
        continuation.allowRepeat shouldBe false
        continuation.selectedModeIndices shouldBe listOf(0)
        continuation.availableIndices.shouldNotBeNull()
        continuation.availableIndices.toSet() shouldBe setOf(1, 2)
    }

    test("F2 — allowRepeat=true: same mode can be picked twice and its effect fires twice on resolution") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val handSizeBefore = d.state.getHand(p1).size
        val spell = d.putCardInHand(p1, "Test Repeat Modal")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        val firstDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // allowRepeat=true: all three modes offered, no narrowing.
        firstDecision.options shouldHaveSize 3
        d.submitDecision(p1, OptionChosenResponse(firstDecision.id, 0))  // Draw a card

        val secondDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Still three offered — mode 0 was NOT narrowed out.
        secondDecision.options shouldHaveSize 3
        secondDecision.options shouldContain "Draw a card"

        // Continuation reflects allowRepeat semantics: no narrowed availableIndices.
        val continuation = d.state.continuationStack
            .filterIsInstance<CastModalModeSelectionContinuation>()
            .single()
        continuation.allowRepeat shouldBe true
        continuation.selectedModeIndices shouldBe listOf(0)
        continuation.availableIndices shouldBe null

        // Pick mode 0 again. Spell now has chosenModes = [0, 0] — illegal without allowRepeat,
        // legal here. Since mode 0 (draw) has no targets, the spell should transition straight
        // to the stack / resolve via auto-drain.
        d.submitDecision(p1, OptionChosenResponse(secondDecision.id, 0))

        // Both players pass until stack resolves; mode 0 ("Draw a card") fires twice.
        // The spell itself left the hand when cast, so hand transitions are:
        //   handSizeBefore (before putCardInHand)
        //   +1 putCardInHand → spell now in hand
        //   -1 cast           → spell on stack
        //   +2 resolve        → mode 0 drew twice
        // Net: handSizeAfter == handSizeBefore + 2.
        d.bothPass()

        val handSizeAfter = d.state.getHand(p1).size
        handSizeAfter shouldBe (handSizeBefore + 2)
    }

    test("F1 regression — picking the last-remaining mode twice fails when !allowRepeat") {
        // Narrows the hole: if someone ever weakens the resumer's narrowing logic, this
        // test will catch it by observing that the second decision does NOT list the first
        // pick, and therefore optionIndex 1 cannot resolve to the originally-picked mode.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(p1, Color.GREEN, 1)

        val spell = d.putCardInHand(p1, "Test Non-Repeat Modal")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        val firstDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Pick mode 1 ("Gain 1 life") at offered-position 1.
        d.submitDecision(p1, OptionChosenResponse(firstDecision.id, 1))

        val secondDecision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        secondDecision.options shouldNotContain "Gain 1 life"
        // Only mode 0 and mode 2 remain; pick mode 0 ("Draw a card") from the narrowed list.
        d.submitDecision(p1, OptionChosenResponse(secondDecision.id, secondDecision.options.indexOf("Draw a card")))

        // The continuation should now have transitioned past mode selection entirely.
        d.state.continuationStack.filterIsInstance<CastModalModeSelectionContinuation>() shouldBe emptyList()
    }
})
