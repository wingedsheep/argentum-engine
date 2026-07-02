package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bumi, King of Three Trials (TLA) — {5}{G} Legendary Creature 4/4.
 *
 *   When Bumi enters, choose up to X, where X is the number of Lesson cards in your graveyard —
 *   • Put three +1/+1 counters on Bumi.
 *   • Target player scries 3.
 *   • Earthbend 3.
 *
 * Exercises the dynamic "choose up to X" modal ETB ([ModalEffect.chooseUpToDynamic] with a
 * [DynamicAmount.Count] cap of Lesson cards in the graveyard) combined with per-mode targets — the
 * +1/+1-counter mode has no target, the scry mode targets a player, and the Earthbend mode targets
 * a land you control. Each mode's target is only demanded when that mode is chosen.
 */
class BumiKingOfThreeTrialsScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val countersMode = "+1/+1 counters on Bumi"
    private val scryMode = "scries 3"
    private val earthbendMode = "Earthbend 3"

    /** Pick the offered mode whose description contains [substr] (robust to option ordering). */
    private fun TestGame.chooseModeContaining(substr: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.contains(substr) }
        check(index >= 0) { "Mode containing '$substr' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        test("with no Lessons in graveyard, X=0 — Bumi just enters, no modes are chosen") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Bumi, King of Three Trials")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bumi, King of Three Trials").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // X = 0 → the modal effect resolves as a no-op; no choice is presented.
            game.hasPendingDecision() shouldBe false
            val bumi = game.findPermanent("Bumi, King of Three Trials")
            bumi.shouldNotBeNull()

            // Base 4/4 — no counters were added.
            val projected = projector.project(game.state)
            projected.getPower(bumi) shouldBe 4
            projected.getToughness(bumi) shouldBe 4
        }

        test("with 2 Lessons, X=2 — choose the +3 counters mode and Earthbend 3") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Bumi, King of Three Trials")
                .withLandsOnBattlefield(1, "Forest", 7)
                .withCardInGraveyard(1, "Firebending Lesson")
                .withCardInGraveyard(1, "Earthbending Lesson")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bumi, King of Three Trials").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // First of the two picks (X capped at min(2, 3 modes) = 2): three +1/+1 counters.
            game.chooseModeContaining(countersMode)
            // Second pick: Earthbend 3.
            game.chooseModeContaining(earthbendMode)

            // Only the Earthbend mode demands a target — a land you control.
            val land = game.findPermanents("Forest").first()
            game.selectTargets(listOf(land)).error shouldBe null

            game.resolveStack()

            val projected = projector.project(game.state)
            val bumi = game.findPermanent("Bumi, King of Three Trials")!!
            // 4/4 base + three +1/+1 counters = 7/7.
            projected.getPower(bumi) shouldBe 7
            projected.getToughness(bumi) shouldBe 7

            // The chosen land is now a 0/0 creature-land with three +1/+1 counters (a 3/3) and haste.
            projected.hasType(land, "LAND") shouldBe true
            projected.hasType(land, "CREATURE") shouldBe true
            projected.getPower(land) shouldBe 3
            projected.getToughness(land) shouldBe 3
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
        }

        test("regression: choosing scry then Earthbend — the scry's mid-resolution pause must not drop the Earthbend mode") {
            // The bug: with scry chosen before Earthbend, the scry mode paused mid-resolution for
            // its reorder decision, and the remaining Earthbend mode was silently dropped — the
            // player never got to target a land. A ModalChosenModeTailContinuation now sits beneath
            // the scry's pause so the queue resumes afterward.
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Bumi, King of Three Trials")
                .withLandsOnBattlefield(1, "Forest", 7)
                .withCardInGraveyard(1, "Firebending Lesson")
                .withCardInGraveyard(1, "Earthbending Lesson")
                // Non-empty library so "scry 3" actually pauses for a reorder decision.
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bumi, King of Three Trials").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // X = 2 → two picks. Choose scry FIRST, then Earthbend.
            game.chooseModeContaining(scryMode)
            game.chooseModeContaining(earthbendMode)

            // Scry mode's player target (two players → an explicit target decision).
            game.selectTargets(listOf(game.player1Id)).error shouldBe null

            // Scry pauses mid-resolution: first choose none of the looked-at cards to put on
            // the bottom, then keep the remaining cards in their current order on top.
            game.getPendingDecision().shouldNotBeNull()
            game.skipSelection()
            game.keepLibraryOrder()

            // The Earthbend mode survived the scry pause and now demands its land target.
            val afterScry = game.getPendingDecision()
            afterScry.shouldNotBeNull()
            afterScry.shouldBeInstanceOf<ChooseTargetsDecision>()

            val land = game.findPermanents("Forest").first()
            game.selectTargets(listOf(land)).error shouldBe null
            game.resolveStack()

            // Earthbend applied: the chosen land is now a 3/3 creature-land with haste.
            val projected = projector.project(game.state)
            projected.hasType(land, "LAND") shouldBe true
            projected.hasType(land, "CREATURE") shouldBe true
            projected.getPower(land) shouldBe 3
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
        }

        test("with 1 Lesson, X=1 — choosing the scry mode targets a player and resolves") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Bumi, King of Three Trials")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInGraveyard(1, "Firebending Lesson")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bumi, King of Three Trials").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // X = 1 → exactly one pick offered; choose "Target player scries 3".
            game.chooseModeContaining(scryMode)

            // The scry mode targets a player — scry yourself. Player 1's library is empty,
            // so the scry looks at zero cards and resolves cleanly (CR 701.22).
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.resolveStack()

            val bumi = game.findPermanent("Bumi, King of Three Trials")
            bumi.shouldNotBeNull()
            // No counter mode chosen — Bumi is still a base 4/4.
            val projected = projector.project(game.state)
            projected.getPower(bumi) shouldBe 4
            game.hasPendingDecision() shouldBe false
        }
    }
}
