package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `ExileAndReturnTransformedEffect.controllerOverride` — who controls the new object an
 * "exile it, then return it to the battlefield transformed" puts onto the battlefield.
 *
 * Without an override it enters under its owner's control (the FIN eikons). With
 * `underYourControl = true` it enters under the ability's controller (Conqueror's Galleon's
 * "…transformed under your control"), which differs when the controller doesn't own it.
 */
class ExileAndReturnTransformedControllerTest : ScenarioTestBase() {

    private fun flippingDfc(name: String, underYourControl: Boolean): CardDefinition =
        CardDefinition.doubleFacedPermanent(
            frontFace = card(name) {
                manaCost = "{1}"
                typeLine = "Artifact"
                oracleText = "{0}: Exile this artifact, then return it to the battlefield transformed."
                activatedAbility {
                    cost = Costs.Free
                    effect = Effects.ExileAndReturnTransformed(underYourControl = underYourControl)
                }
            },
            backFace = card("$name Back") {
                manaCost = ""
                typeLine = "Land"
                oracleText = ""
            }
        )

    init {
        cardRegistry.register(
            listOf(
                flippingDfc("Owner Flip Relic", underYourControl = false),
                flippingDfc("Controller Flip Relic", underYourControl = true)
            )
        )

        fun flipStolen(name: String): TestGame {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, name) // owned by player 2
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val relic = game.findPermanent(name)!!
            // Player 1 controls the opponent-owned artifact, so the activated ability is theirs.
            game.state = game.state.updateEntity(relic) { it.with(ControllerComponent(game.player1Id)) }
            val abilityId = cardRegistry.getCard(name)!!.activatedAbilities.single().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = relic, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()
            return game
        }

        test("without an override the transformed permanent returns under its owner's control") {
            val game = flipStolen("Owner Flip Relic")
            val returned = game.findPermanent("Owner Flip Relic Back").shouldNotBeNull()
            game.state.projectedState.getController(returned) shouldBe game.player2Id
        }

        test("under your control returns it under the ability's controller, not the owner") {
            val game = flipStolen("Controller Flip Relic")
            val returned = game.findPermanent("Controller Flip Relic Back").shouldNotBeNull()
            game.state.projectedState.getController(returned) shouldBe game.player1Id
            game.state.getZone(game.player2Id, Zone.EXILE).contains(returned) shouldBe false
        }
    }
}
