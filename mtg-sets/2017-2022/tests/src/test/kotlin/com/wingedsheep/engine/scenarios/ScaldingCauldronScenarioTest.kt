package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scalding Cauldron (ELD #229) — {1} Artifact.
 *
 * "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature."
 */
class ScaldingCauldronScenarioTest : ScenarioTestBase() {

    init {
        context("Scalding Cauldron") {

            test("sacrificing it deals 3 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scalding Cauldron")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cauldron = game.findPermanent("Scalding Cauldron")!!
                val giant = game.findPermanent("Hill Giant")!!
                val abilityId = cardRegistry.getCard("Scalding Cauldron")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cauldron,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null

                withClue("the Cauldron is sacrificed as a cost") {
                    game.isInGraveyard(1, "Scalding Cauldron") shouldBe true
                }

                game.resolveStack()

                withClue("Hill Giant (3/3) took 3 damage and died") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
