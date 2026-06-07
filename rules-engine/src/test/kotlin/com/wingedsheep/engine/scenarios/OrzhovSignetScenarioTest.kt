package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Orzhov Signet (GPT #155) — {2} Artifact.
 *
 * "{1}, {T}: Add {W}{B}."
 *
 * Verifies the mana ability: paying {1} (from a Swamp) plus tapping the Signet adds {W}{B}
 * to the controller's mana pool.
 */
class OrzhovSignetScenarioTest : ScenarioTestBase() {

    private val signetAbilityId by lazy {
        cardRegistry.requireCard("Orzhov Signet").activatedAbilities[0].id
    }

    init {
        context("Orzhov Signet mana ability") {

            test("paying {1} and tapping adds {W}{B} to the pool") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orzhov Signet", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Fill the pool with one {B} from the Swamp to pay the {1} activation cost.
                val swamp = game.findPermanent("Swamp")!!
                val swampAbility = cardRegistry.requireCard("Swamp").activatedAbilities[0].id
                game.execute(ActivateAbility(game.player1Id, swamp, swampAbility)).error shouldBe null

                val signet = game.findPermanent("Orzhov Signet")!!
                val result = game.execute(ActivateAbility(game.player1Id, signet, signetAbilityId))
                withClue("Activating Orzhov Signet should succeed: ${result.error}") { result.error shouldBe null }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Signet should net one white mana") { pool.white shouldBe 1 }
                withClue("Signet should net one black mana (the {B} from Swamp was spent on {1})") {
                    pool.black shouldBe 1
                }
            }
        }
    }
}
