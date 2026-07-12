package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Blood Fountain (VOW #95) — {B} Artifact.
 *
 *   When this artifact enters, create a Blood token.
 *   {3}{B}, {T}, Sacrifice this artifact: Return up to two target creature cards from your
 *   graveyard to your hand.
 *
 * Exercises the ETB Blood token creation and the tap-sacrifice activated ability returning two
 * targeted creature cards from the graveyard to hand.
 */
class BloodFountainScenarioTest : ScenarioTestBase() {

    init {
        context("Blood Fountain") {

            test("entering the battlefield creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blood Fountain")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blood Fountain").error shouldBe null
                game.resolveStack()

                withClue("a Blood token is created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("the {3}{B}, tap, sacrifice ability returns two graveyard creatures to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blood Fountain")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fountain = game.findPermanent("Blood Fountain")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val abilityId = cardRegistry.getCard("Blood Fountain")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = fountain,
                        abilityId = abilityId,
                        targets = listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)
                        )
                    )
                )
                withClue("Activating the ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Blood Fountain was sacrificed to pay the cost") {
                    game.isOnBattlefield("Blood Fountain") shouldBe false
                    game.isInGraveyard(1, "Blood Fountain") shouldBe true
                }
                withClue("Both creature cards returned to hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe true
                }
                withClue("Both are gone from the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Hill Giant") shouldBe false
                }
            }
        }
    }
}
