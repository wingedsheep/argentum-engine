package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.TowerOfMurmurs
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tower of Murmurs (MRD #268, {4}, Artifact).
 *
 *   {8}, {T}: Target player mills eight cards.
 *
 * Eight generic and a tap is a steep price, so the two things worth pinning down are that the mill
 * lands on the *targeted* player and that a library shorter than eight simply empties out rather
 * than erroring — the loss comes later, on the next draw from an empty library (CR 104.3c).
 */
class TowerOfMurmursScenarioTest : ScenarioTestBase() {

    private val abilityId = TowerOfMurmurs.activatedAbilities.single().id

    init {
        context("Tower of Murmurs") {

            test("mills eight cards from the targeted opponent") {
                var builder = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Tower of Murmurs")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val tower = game.findPermanent("Tower of Murmurs")!!
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tower,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Activating the Tower should succeed: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Bob's library should be eight cards lighter") {
                    game.librarySize(2) shouldBe 2
                }
                withClue("Those eight cards land in Bob's graveyard") {
                    game.graveyardSize(2) shouldBe 8
                }
                withClue("Alice's own library is untouched") {
                    game.graveyardSize(1) shouldBe 0
                }
            }

            test("a library shorter than eight just empties out") {
                var builder = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Tower of Murmurs")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val tower = game.findPermanent("Tower of Murmurs")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tower,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("All three cards are milled, and nothing errors on the shortfall") {
                    game.librarySize(2) shouldBe 0
                    game.graveyardSize(2) shouldBe 3
                }
            }
        }
    }
}
