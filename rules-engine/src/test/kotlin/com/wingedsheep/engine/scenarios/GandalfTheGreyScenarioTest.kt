package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gandalf the Grey — "Whenever you cast an instant or sorcery spell, choose one that hasn't
 * been chosen —" with the four-mode modal-with-memory ability.
 *
 * Covers:
 *  - casting an instant triggers the modal choice;
 *  - the "deal 3 damage to each opponent" mode hits the opponent;
 *  - the chosen mode is no longer offered on a later trigger ("hasn't been chosen");
 *  - the copy mode copies a controlled instant on the stack.
 */
class GandalfTheGreyScenarioTest : ScenarioTestBase() {

    private val damageMode = "Gandalf deals 3 damage to each opponent"
    private val copyMode = "Copy target instant or sorcery spell you control. You may choose new targets for the copy"

    /** Resolve the stack until the modal ChooseOptionDecision appears. */
    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        resolveStack()
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        test("casting an instant triggers the choice; damage mode hits each opponent") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gandalf the Grey")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Lightning Bolt at the opponent's face — Gandalf triggers on the cast.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null

            // The trigger resolves first → modal choice with all four modes offered.
            val choice = game.resolveToModeChoice()
            choice.options.size shouldBe 4
            choice.options shouldContain damageMode

            game.chooseMode(choice, damageMode)

            // Resolve the rest (the Bolt then deals its 3). 3 (Gandalf) + 3 (Bolt) = 6.
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 14
        }

        test("a chosen mode is no longer offered on a later trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gandalf the Grey")
                .withCardsInHand(1, "Lightning Bolt", 2)
                .withLandsOnBattlefield(1, "Mountain", 8)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // First instant: choose the damage mode.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            val first = game.resolveToModeChoice()
            first.options.size shouldBe 4
            game.chooseMode(first, damageMode)
            game.resolveStack()

            // Second instant: the damage mode must NOT be offered again.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            val second = game.resolveToModeChoice()
            second.options.size shouldBe 3
            second.options shouldNotContain damageMode
        }

        test("copy mode copies a controlled instant on the stack") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gandalf the Grey")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Lightning Bolt at the opponent. The Bolt sits on the stack below the
            // Gandalf trigger, so it's a legal "instant you control" to copy.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null

            val choice = game.resolveToModeChoice()
            game.chooseMode(choice, copyMode)

            // The copy mode needs a target spell — pick the Bolt on the stack.
            val boltSpell = game.state.stack.first { id ->
                game.state.getEntity(id)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Lightning Bolt"
            }
            game.selectTargets(listOf(boltSpell)).error shouldBe null

            // The copy may be retargeted — keep it on the opponent's face.
            var guard = 0
            while (game.state.stack.isNotEmpty() && guard++ < 12) {
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id))
                }
            }

            // Original Bolt (3) + copy (3) = 6 damage.
            game.getLifeTotal(2) shouldBe 14
        }
    }
}
