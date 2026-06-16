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
 * Scenario test for Ashnod's Altar (ATQ).
 *
 * {3} Artifact
 * "Sacrifice a creature: Add {C}{C}."
 */
class AshnodsAltarScenarioTest : ScenarioTestBase() {

    init {
        context("Ashnod's Altar") {

            test("sacrificing a creature adds two colorless mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashnod's Altar")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val altarId = game.findPermanent("Ashnod's Altar")!!
                val creatureId = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Ashnod's Altar")!!.script.activatedAbilities[0]

                val colorlessBefore =
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless ?: 0

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = altarId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(creatureId))
                    )
                )
                withClue("Activating Ashnod's Altar should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed creature should be gone") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Two colorless mana should have been added to the pool") {
                    (pool?.colorless ?: 0) shouldBe colorlessBefore + 2
                }
            }
        }
    }
}
