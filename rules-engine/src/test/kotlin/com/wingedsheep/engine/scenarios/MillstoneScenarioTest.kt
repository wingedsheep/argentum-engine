package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Millstone (ATQ).
 *
 * {2} Artifact
 * "{2}, {T}: Target player mills two cards."
 */
class MillstoneScenarioTest : ScenarioTestBase() {

    init {
        context("Millstone") {

            test("activating Millstone mills two cards from the targeted player's library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Millstone")
                    // Lands to pay the {2} portion; {T} taps Millstone automatically.
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Target player's library to mill from.
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val millstoneId = game.findPermanent("Millstone")!!
                val ability = cardRegistry.getCard("Millstone")!!.script.activatedAbilities[0]

                val libBefore = game.librarySize(2)
                val graveBefore = game.graveyardSize(2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = millstoneId,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id))
                    )
                )
                withClue("Activating Millstone should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Two cards should move from library to graveyard") {
                    game.librarySize(2) shouldBe libBefore - 2
                    game.graveyardSize(2) shouldBe graveBefore + 2
                }
            }
        }
    }
}
