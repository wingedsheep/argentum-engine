package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Ghired, Mirror of the Wilds.
 *
 * Ghired, Mirror of the Wilds ({R}{G}{W}): Legendary Creature — Human Shaman, 3/3, Haste.
 * "Nontoken creatures you control have '{T}: Create a token that's a copy of target token
 * you control that entered this turn.'"
 */
class GhiredMirrorOfTheWildsScenarioTest : ScenarioTestBase() {

    private fun grantedAbilityId() =
        cardRegistry.getCard("Ghired, Mirror of the Wilds")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().first().ability.id

    init {
        context("Ghired grants nontoken creatures the token-copy ability") {

            test("a nontoken creature can tap to copy a token that entered this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ghired, Mirror of the Wilds")
                    // A nontoken creature that receives the granted ability.
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // A token that "entered this turn" — the legal target.
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ghired = game.findPermanent("Ghired, Mirror of the Wilds")!!
                // Distinguish the nontoken bears from the token bears.
                val bearsIds = game.getClientState(1).cards.values
                    .filter { it.name == "Grizzly Bears" }
                    .map { it.id }
                val nontokenBears = bearsIds.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() != true
                }
                val tokenBears = bearsIds.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() == true
                }

                // Mark the token as having entered this turn.
                game.state = game.state.updateEntity(tokenBears) { c ->
                    c.with(EnteredThisTurnComponent)
                }

                // The nontoken bears (not Ghired itself the only one — but any nontoken
                // creature you control) must have the granted ability available.
                val tokenCountBefore = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
                    .count { game.state.getEntity(it)?.has<TokenComponent>() == true }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nontokenBears,
                        abilityId = grantedAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(tokenBears)),
                    ),
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val tokenCountAfter = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
                    .count { game.state.getEntity(it)?.has<TokenComponent>() == true }

                withClue("A new token copy should have been created") {
                    tokenCountAfter shouldBe tokenCountBefore + 1
                }
                // Ghired is unrelated to whether it taps — make sure it wasn't consumed.
                game.state.getEntity(ghired) shouldNotBe null
            }

            test("a token that did NOT enter this turn is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ghired, Mirror of the Wilds")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsIds = game.getClientState(1).cards.values
                    .filter { it.name == "Grizzly Bears" }
                    .map { it.id }
                val nontokenBears = bearsIds.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() != true
                }
                val tokenBears = bearsIds.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() == true
                }

                // Note: we deliberately do NOT stamp EnteredThisTurnComponent on the token.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nontokenBears,
                        abilityId = grantedAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(tokenBears)),
                    ),
                )
                withClue("Activation should fail: token did not enter this turn") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
