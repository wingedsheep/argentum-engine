package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Open the Way — "X can't be greater than the number of players in the game. Reveal cards from
 * the top of your library until you reveal X land cards. Put those land cards onto the
 * battlefield tapped and the rest on the bottom of your library in a random order."
 */
class OpenTheWayScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.openTheWayId(): EntityId =
        state.getHand(player1Id).first { nameOf(it) == "Open the Way" }

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Open the Way")
        .withLandsOnBattlefield(1, "Forest", 8)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("in a two-player game X is offered up to 2, however much mana is available") {
            val g = setup()
            val offer = g.getLegalActions(1).first { (it.action as? CastSpell)?.cardId == g.openTheWayId() }
            withClue("eight Forests pay for X = 6") { offer.maxAffordableX shouldBe 2 }
            g.execute(CastSpell(g.player1Id, g.openTheWayId(), xValue = 3)).error.shouldNotBeNull()
        }

        test("X = 2 puts the first two lands revealed onto the battlefield tapped and the rest on the bottom") {
            val g = setup()
            val libraryForest = g.state.getLibrary(g.player1Id).first { g.nameOf(it) == "Forest" }
            val libraryIsland = g.state.getLibrary(g.player1Id).first { g.nameOf(it) == "Island" }
            g.execute(CastSpell(g.player1Id, g.openTheWayId(), xValue = 2)).error shouldBe null
            g.resolveStack()

            for (land in listOf(libraryForest, libraryIsland)) {
                withClue("${g.nameOf(land)} is on the battlefield, tapped") {
                    g.state.getBattlefield().contains(land) shouldBe true
                    g.state.getEntity(land)?.has<TappedComponent>() shouldBe true
                }
            }
            g.findPermanent("Plains") shouldBe null

            val library = g.state.getLibrary(g.player1Id).map { g.nameOf(it) }
            withClue("the unrevealed cards stay on top; the revealed nonlands go under them") {
                library.take(2) shouldBe listOf("Hill Giant", "Plains")
                library.drop(2) shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Shock")
            }
        }
    }
}
