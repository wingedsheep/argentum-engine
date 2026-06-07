package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ostiary Thrull (GPT #55) — {3}{B} Creature — Thrull, 2/2.
 *
 * "{W}, {T}: Tap target creature."
 *
 * Verifies the activated ability taps the targeted creature when {W} is paid and the Thrull taps.
 */
class OstiaryThrullScenarioTest : ScenarioTestBase() {

    private val tapAbilityId by lazy {
        cardRegistry.requireCard("Ostiary Thrull").activatedAbilities[0].id
    }

    init {
        context("Ostiary Thrull tap ability") {

            test("paying {W} and tapping the Thrull taps the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ostiary Thrull", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Centaur Courser", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Fill the pool with {W} to pay the activation cost.
                val plains = game.findPermanent("Plains")!!
                val plainsAbility = cardRegistry.requireCard("Plains").activatedAbilities[0].id
                game.execute(ActivateAbility(game.player1Id, plains, plainsAbility)).error shouldBe null

                val thrull = game.findPermanent("Ostiary Thrull")!!
                val courser = game.findPermanent("Centaur Courser")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = thrull,
                        abilityId = tapAbilityId,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                )
                withClue("Activating Ostiary Thrull's tap ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Targeted creature should be tapped") {
                    game.state.getEntity(courser)?.has<TappedComponent>() shouldBe true
                }
                withClue("Ostiary Thrull should be tapped from paying its {T} cost") {
                    game.state.getEntity(thrull)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
