package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Kessig Wolfrider (VOW #165) — {R} Creature — Human Knight, 1/2.
 *
 *   Menace
 *   {2}{R}, {T}, Exile three cards from your graveyard: Create a 3/2 red Wolf creature token.
 *
 * Exercises the printed Menace keyword and the tap + mana + exile-three-from-graveyard activated
 * ability that creates a 3/2 red Wolf token.
 */
class KessigWolfriderScenarioTest : ScenarioTestBase() {

    init {
        context("Kessig Wolfrider") {

            test("has Menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kessig Wolfrider", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolfrider = game.findPermanent("Kessig Wolfrider")!!
                withClue("Kessig Wolfrider has Menace") {
                    game.state.projectedState.hasKeyword(wolfrider, Keyword.MENACE) shouldBe true
                }
            }

            test("{2}{R}, {T}, exile three cards from graveyard creates a 3/2 red Wolf token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kessig Wolfrider", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolfrider = game.findPermanent("Kessig Wolfrider")!!
                val abilityId = cardRegistry.getCard("Kessig Wolfrider")!!.activatedAbilities.first().id
                val wolvesBefore = game.findPermanents("Wolf Token").size
                val graveyardBefore = game.graveyardSize(1)

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wolfrider,
                        abilityId = abilityId
                    )
                )
                withClue("activation should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("three cards were exiled from the graveyard") {
                    game.graveyardSize(1) shouldBe graveyardBefore - 3
                }
                withClue("a 3/2 red Wolf token is created") {
                    game.findPermanents("Wolf Token").size shouldBe wolvesBefore + 1
                }
            }
        }
    }
}
