package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Floodhound (MH2 #42) — {U} Creature — Elemental Dog, 1/2.
 *
 *   {3}, {T}: Investigate.
 */
class FloodhoundScenarioTest : ScenarioTestBase() {

    init {
        context("Floodhound's investigate ability") {

            test("{3}, {T} taps Floodhound and creates a Clue") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Floodhound", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hound = game.findPermanent("Floodhound")!!
                val abilityId = cardRegistry.getCard("Floodhound")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hound, abilityId = abilityId)
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("exactly one Clue was created") {
                    game.findAllPermanents("Clue").size shouldBe 1
                }
                withClue("Floodhound is tapped as part of the cost") {
                    game.state.getEntity(hound)?.has<TappedComponent>() shouldBe true
                }
            }

            test("can't be activated with summoning sickness") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Floodhound", summoningSickness = true)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hound = game.findPermanent("Floodhound")!!
                val abilityId = cardRegistry.getCard("Floodhound")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hound, abilityId = abilityId)
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Clue") shouldBe false
            }

            test("can't be activated without {3} available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Floodhound", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hound = game.findPermanent("Floodhound")!!
                val abilityId = cardRegistry.getCard("Floodhound")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hound, abilityId = abilityId)
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Clue") shouldBe false
            }
        }
    }
}
