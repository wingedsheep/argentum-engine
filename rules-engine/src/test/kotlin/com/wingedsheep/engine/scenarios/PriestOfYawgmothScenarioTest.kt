package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Priest of Yawgmoth (ATQ #19) — {1}{B} 1/2 creature.
 *
 * "{T}, Sacrifice an artifact: Add an amount of {B} equal to the sacrificed artifact's mana value."
 *
 * Composes entirely from existing primitives (no engine work): the sacrifice cost binds the
 * sacrificed artifact to EntityReference.Sacrificed(0); AddMana reads its mana value via
 * DynamicAmount.EntityProperty. Pins that the {B} produced equals the sacrificed artifact's MV
 * across {0} (no mana), a mid-MV artifact, and a high-MV artifact.
 */
class PriestOfYawgmothScenarioTest : ScenarioTestBase() {

    init {
        // Sacrifice an artifact of known mana value, return the {B} added to the pool.
        fun blackProducedBySacrificing(artifactName: String): Int {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Priest of Yawgmoth", summoningSickness = false)
                .withCardOnBattlefield(1, artifactName)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val priest = game.findPermanent("Priest of Yawgmoth")!!
            val artifact = game.findPermanent(artifactName)!!
            val ability = cardRegistry.getCard("Priest of Yawgmoth")!!.script.activatedAbilities[0]

            val blackBefore = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black ?: 0

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = priest,
                    abilityId = ability.id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifact))
                )
            )
            withClue("Activating Priest of Yawgmoth should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("the sacrificed artifact is gone") {
                game.isOnBattlefield(artifactName) shouldBe false
            }
            val blackAfter = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black ?: 0
            return blackAfter - blackBefore
        }

        context("Priest of Yawgmoth") {

            test("sacrificing a mana-value-0 artifact (Ornithopter) adds no black mana") {
                blackProducedBySacrificing("Ornithopter") shouldBe 0
            }

            test("sacrificing a mana-value-2 artifact (Millstone) adds {B}{B}") {
                // Millstone is {2} → mana value 2.
                blackProducedBySacrificing("Millstone") shouldBe 2
            }

            test("sacrificing a high-mana-value artifact (Colossus of Sardia, {9}) adds nine {B}") {
                // Colossus of Sardia is {9} → mana value 9.
                blackProducedBySacrificing("Colossus of Sardia") shouldBe 9
            }
        }
    }
}
