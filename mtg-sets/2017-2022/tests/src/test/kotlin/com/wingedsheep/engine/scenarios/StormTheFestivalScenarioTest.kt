package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Storm the Festival (MID #200) — {3}{G}{G}{G} sorcery. "Look at the top five cards of your
 * library. You may put up to two permanent cards with mana value 5 or less from among them onto
 * the battlefield. Put the rest on the bottom of your library in a random order." Flashback
 * {7}{G}{G}{G}.
 */
class StormTheFestivalScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    init {
        context("Storm the Festival") {

            test("two permanents with mana value 5 or less enter the battlefield; the rest go to the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Storm the Festival")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Ornithopter")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Storm the Festival").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Craw Wurm (MV 6) is not eligible; the land and MV<=5 creatures are") {
                    decision.options.map { game.nameOf(it) } shouldContainExactlyInAnyOrder
                        listOf("Grizzly Bears", "Hill Giant", "Ornithopter", "Forest")
                }

                val chosen = decision.options.filter { game.nameOf(it) in setOf("Grizzly Bears", "Hill Giant") }
                game.submitDecision(
                    CardsSelectedResponse(decisionId = decision.id, selectedCards = chosen)
                ).error shouldBe null
                game.resolveStack()

                withClue("chosen permanents are on the battlefield") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                    (game.findPermanent("Hill Giant") != null) shouldBe true
                    (game.findPermanent("Craw Wurm") != null) shouldBe false
                }
                withClue("the other three went to the bottom of the library") {
                    game.librarySize(1) shouldBe 3
                }
                game.isInGraveyard(1, "Storm the Festival") shouldBe true
            }
        }
    }
}
