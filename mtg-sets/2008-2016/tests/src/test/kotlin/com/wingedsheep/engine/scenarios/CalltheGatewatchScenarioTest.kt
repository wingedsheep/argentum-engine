package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Call the Gatewatch — {2}{W} Sorcery. "Search your library for a planeswalker card, reveal it,
 * put it into your hand, then shuffle."
 */
class CalltheGatewatchScenarioTest : ScenarioTestBase() {
    init {
        test("finds only a planeswalker card and puts it into your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Call the Gatewatch")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Chandra, Torch of Defiance")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val chandra = game.findCardsInLibrary(1, "Chandra, Torch of Defiance").single()

            game.castSpell(1, "Call the Gatewatch").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as? SelectCardsDecision
                ?: error("expected a search selection, got ${game.getPendingDecision()}")
            withClue("only the planeswalker is findable") {
                decision.options shouldContainExactly listOf(chandra)
            }
            game.selectCards(listOf(chandra)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Chandra, Torch of Defiance") shouldBe true
            game.librarySize(1) shouldBe 1
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
        }
    }
}
