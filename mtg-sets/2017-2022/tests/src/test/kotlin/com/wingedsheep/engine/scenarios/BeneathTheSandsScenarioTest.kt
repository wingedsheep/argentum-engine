package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Beneath the Sands (HOU) — {2}{G} Sorcery. Fetches a basic land onto the battlefield tapped;
 * cycling {2}.
 */
class BeneathTheSandsScenarioTest : ScenarioTestBase() {
    init {
        test("puts a basic land from the library onto the battlefield tapped; nonbasics aren't offered") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Beneath the Sands")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Azorius Guildgate")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Beneath the Sands").error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision() as? SelectCardsDecision
                ?: error("expected a library search; got ${game.getPendingDecision()}")
            withClue("only the basic land is offered, and at most one") {
                search.options.size shouldBe 1
                search.maxSelections shouldBe 1
            }
            game.selectCards(search.options).error shouldBe null
            game.resolveStack()

            val plains = game.findPermanent("Plains") ?: error("Plains should be on the battlefield")
            game.state.getEntity(plains)?.has<TappedComponent>() shouldBe true
            game.isInGraveyard(1, "Beneath the Sands") shouldBe true
        }

        test("cycles for {2} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Beneath the Sands").withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Beneath the Sands").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Beneath the Sands") shouldBe true
            game.isInHand(1, "Island") shouldBe true
        }
    }
}
