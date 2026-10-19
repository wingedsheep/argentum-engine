package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Land Equilibrium: "If an opponent who controls at least as many lands as you do would put a land
 * onto the battlefield, that player instead puts that land onto the battlefield then sacrifices a
 * land of their choice."
 */
class LandEquilibriumScenarioTest : ScenarioTestBase() {

    private fun board(theirLands: Int, vararg hand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Land Equilibrium")
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(2, "Forest", theirLands)
        .let { b -> hand.fold(b) { acc, name -> acc.withCardInHand(2, name) } }
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("an opponent at parity plays a land and sacrifices a land of their choice") {
            val game = board(theirLands = 3, "Mountain")
            game.execute(PlayLand(game.player2Id, game.findCardsInHand(2, "Mountain").single())).error shouldBe null
            withClue("the opponent chooses which land to sacrifice") {
                game.state.pendingDecision?.playerId shouldBe game.player2Id
            }
            game.selectCards(listOf(game.findPermanents("Forest").first())).error shouldBe null
            game.isOnBattlefield("Mountain") shouldBe true
            game.findPermanents("Forest").size shouldBe 2
        }

        test("an opponent with fewer lands keeps the land") {
            val game = board(theirLands = 2, "Mountain")
            game.execute(PlayLand(game.player2Id, game.findCardsInHand(2, "Mountain").single())).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.findPermanents("Forest").size shouldBe 2
            game.isOnBattlefield("Mountain") shouldBe true
        }

        test("a land an opponent's Rampant Growth fetches is replaced too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Land Equilibrium")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInHand(2, "Rampant Growth")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Rampant Growth").error shouldBe null
            game.resolveStack()
            game.selectCards(game.findCardsInLibrary(2, "Plains")).error shouldBe null
            game.hasPendingDecision() shouldBe true
            game.selectCards(game.findPermanents("Plains")).error shouldBe null
            game.isInGraveyard(2, "Plains") shouldBe true
            game.findPermanents("Forest").size shouldBe 2
        }
    }
}
