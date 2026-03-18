package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Zahid, Djinn of the Lamp.
 *
 * Card reference:
 * - Zahid, Djinn of the Lamp ({4}{U}{U}): Legendary Creature — Djinn 5/6
 *   You may pay {3}{U} and tap an untapped artifact you control rather than
 *   pay this spell's mana cost.
 *   Flying
 */
class ZahidDjinnOfTheLampScenarioTest : ScenarioTestBase() {

    init {
        context("Zahid, Djinn of the Lamp") {

            test("can be cast for normal mana cost {4}{U}{U}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zahid, Djinn of the Lamp")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Zahid, Djinn of the Lamp")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val p1Battlefield = game.state.getBattlefield(game.player1Id)
                val hasZahid = p1Battlefield.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Zahid, Djinn of the Lamp"
                }
                hasZahid shouldBe true
            }

            test("can be cast for alternative cost {3}{U} + tap artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zahid, Djinn of the Lamp")
                    .withLandsOnBattlefield(1, "Island", 4) // Only 4 mana for {3}{U}
                    .withCardOnBattlefield(1, "Aesthir Glider") // Artifact creature
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithSelfAlternativeCost(
                    1, "Zahid, Djinn of the Lamp", "Aesthir Glider"
                )
                withClue("Cast with alternative cost should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Zahid should be on the battlefield
                val p1Battlefield = game.state.getBattlefield(game.player1Id)
                val hasZahid = p1Battlefield.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Zahid, Djinn of the Lamp"
                }
                hasZahid shouldBe true

                // Aesthir Glider should be tapped
                val gliderId = p1Battlefield.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Aesthir Glider"
                }
                game.state.getEntity(gliderId)?.has<TappedComponent>() shouldBe true
            }

            test("cannot use alternative cost if artifact is already tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zahid, Djinn of the Lamp")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Aesthir Glider", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithSelfAlternativeCost(
                    1, "Zahid, Djinn of the Lamp", "Aesthir Glider"
                )
                withClue("Cast should fail because artifact is already tapped") {
                    castResult.isSuccess shouldBe false
                }
            }
        }
    }
}
