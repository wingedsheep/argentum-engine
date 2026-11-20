package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Stymied Hopes — {1}{U} Instant (THS #64).
 *
 * "Counter target spell unless its controller pays {1}. Scry 1."
 */
class StymiedHopesScenarioTest : ScenarioTestBase() {

    private fun bearsOnStack(): TestGame = scenario()
        .withPlayers("Caster", "Counterer")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInHand(2, "Stymied Hopes")
        .withLandsOnBattlefield(2, "Island", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
        }

    init {
        test("counters the spell when its controller declines to pay, then the caster scries 1") {
            val game = bearsOnStack()
            game.castSpellTargetingStackSpell(2, "Stymied Hopes", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)

            val top = game.state.getLibrary(game.player2Id).first()
            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the Stymied Hopes caster scries their own top card") {
                scry.playerId shouldBe game.player2Id
                scry.options shouldBe listOf(top)
            }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player2Id).last() shouldBe top
            }
        }

        test("paying {1} lets the spell resolve") {
            val game = bearsOnStack()
            game.castSpellTargetingStackSpell(2, "Stymied Hopes", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.submitManaSourcesAutoPay()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
