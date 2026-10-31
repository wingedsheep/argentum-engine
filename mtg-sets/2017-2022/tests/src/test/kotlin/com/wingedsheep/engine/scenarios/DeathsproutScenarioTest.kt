package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Deathsprout (WAR #189) — {1}{B}{B}{G} Instant.
 *
 *   Destroy target creature. Search your library for a basic land card, put it onto the
 *   battlefield tapped, then shuffle.
 */
class DeathsproutScenarioTest : ScenarioTestBase() {

    init {
        test("destroys the target creature and ramps a tapped basic land") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Deathsprout")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Deathsprout", giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true

            val forest = game.findCardsInLibrary(1, "Forest").single()
            game.getPendingDecision()?.playerId shouldBe game.player1Id
            game.selectCards(listOf(forest)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
            game.findAllPermanents("Forest").contains(forest) shouldBe true
            game.librarySize(1) shouldBe 1
        }
    }
}
