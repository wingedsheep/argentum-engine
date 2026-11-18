package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rise of Eagles (JOU #49) — {4}{U}{U} Sorcery.
 *
 *   Create two 2/2 blue Bird enchantment creature tokens with flying. Scry 1.
 */
class RiseOfEaglesScenarioTest : ScenarioTestBase() {
    init {
        test("creates two 2/2 flying Bird enchantment creatures, then scries 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Rise of Eagles")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.castSpell(1, "Rise of Eagles").error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") {
                scry.options shouldBe listOf(bears)
            }
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe bears
            }

            val birds = game.findPermanents("Bird Token")
            birds shouldHaveSize 2
            val projected = game.state.projectedState
            birds.forEach { bird ->
                projected.getPower(bird) shouldBe 2
                projected.getToughness(bird) shouldBe 2
                projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
                projected.hasType(bird, "ENCHANTMENT") shouldBe true
                projected.isCreature(bird) shouldBe true
            }
        }
    }
}
