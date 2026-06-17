package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual

/**
 * Scenario tests for Worldwalker Helm (BIG #7, {2}{U}, Artifact).
 *
 *   If you would create one or more artifact tokens, instead create those tokens plus an
 *   additional Map token.
 *   {1}{U}, {T}: Create a token that's a copy of target artifact token you control.
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.CreateAdditionalToken] replacement
 * effect: any artifact-token creation you control gets one extra Map token, and the Map
 * itself (also an artifact token) must NOT recursively re-trigger the effect.
 */
class WorldwalkerHelmScenarioTest : ScenarioTestBase() {

    init {
        context("Worldwalker Helm") {

            test("creating a Treasure also makes one Map (no recursive re-trigger)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Worldwalker Helm")
                    .withCardInHand(1, "Brazen Freebooter")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Brazen Freebooter's ETB creates one Treasure (an artifact token).
                val cast = game.castSpell(1, "Brazen Freebooter")
                withClue("Casting Brazen Freebooter should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                while (game.hasPendingDecision()) game.resolveStack()

                withClue("Brazen Freebooter makes one Treasure token") {
                    tokensNamed(game, "Treasure") shouldBe 1
                }
                withClue("The artifact-token creation triggers exactly ONE additional Map (Map doesn't recurse)") {
                    tokensNamed(game, "Map") shouldBe 1
                }
            }

            test("no Helm: a Treasure creation makes no Map") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Brazen Freebooter")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Brazen Freebooter")
                cast.error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                while (game.hasPendingDecision()) game.resolveStack()

                withClue("Without the Helm there is no Map token") {
                    tokensNamed(game, "Treasure") shouldBe 1
                    tokensNamed(game, "Map") shouldBe 0
                }
            }

            test("copy ability duplicates a target artifact token you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Worldwalker Helm", summoningSickness = false)
                    .withCardInHand(1, "Brazen Freebooter")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Make a Treasure (and Map, via the Helm) to have artifact tokens to copy.
                game.castSpell(1, "Brazen Freebooter").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                while (game.hasPendingDecision()) game.resolveStack()

                val artifactToken = game.state.getBattlefield().first { id ->
                    val e = game.state.getEntity(id) ?: return@first false
                    e.get<TokenComponent>() != null &&
                        e.get<CardComponent>()?.name == "Treasure"
                }
                val before = tokensNamed(game, "Treasure") + tokensNamed(game, "Map")

                val helm = game.findPermanent("Worldwalker Helm")!!
                val copyAbility = cardRegistry.getCard("Worldwalker Helm")!!.script.activatedAbilities.first()
                val act = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = helm,
                        abilityId = copyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(artifactToken)),
                    )
                )
                withClue("Activating the copy ability should succeed: ${act.error}") { act.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                while (game.hasPendingDecision()) game.resolveStack()

                withClue("Copying an artifact token adds at least one more artifact token") {
                    (tokensNamed(game, "Treasure") + tokensNamed(game, "Map")) shouldBeGreaterThanOrEqual (before + 1)
                }
            }
        }
    }

    private fun tokensNamed(game: TestGame, name: String): Int =
        game.state.getBattlefield().count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
}
