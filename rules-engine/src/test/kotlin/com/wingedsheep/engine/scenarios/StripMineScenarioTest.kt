package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Strip Mine (ATQ).
 *
 * Land
 * "{T}: Add {C}."
 * "{T}, Sacrifice this land: Destroy target land."
 */
class StripMineScenarioTest : ScenarioTestBase() {

    init {
        context("Strip Mine") {

            test("the sacrifice ability destroys a target land and sacrifices Strip Mine") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Strip Mine", summoningSickness = false)
                    // A land controlled by player 2 to destroy.
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stripMineId = game.findPermanent("Strip Mine")!!
                val targetLandId = game.findPermanent("Forest")!!
                // Index 0 is the mana ability ({T}: Add {C}); index 1 is the sacrifice/destroy ability.
                val ability = cardRegistry.getCard("Strip Mine")!!.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = stripMineId,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, targetLandId))
                    )
                )
                withClue("Activating Strip Mine's destroy ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The targeted land should be destroyed") {
                    game.isOnBattlefield("Forest") shouldBe false
                }
                withClue("Strip Mine should be sacrificed") {
                    game.isOnBattlefield("Strip Mine") shouldBe false
                }
            }
        }
    }
}
