package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import io.kotest.matchers.shouldBe

class OmniscienceScenarioTest : ScenarioTestBase() {

    private val calculator = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator)

    private fun com.wingedsheep.engine.state.GameState.cardInHand(playerId: EntityId, name: String): EntityId =
        getHand(playerId).first { getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Omniscience free-cast permission") {

            test("its controller may cast a spell from hand without paying its mana cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Omniscience")
                    .withCardInHand(1, "Grizzly Bears")
                    .build()
                val player = game.player1Id
                val bears = game.state.cardInHand(player, "Grizzly Bears")

                calculator.hasFreeCastPermission(
                    game.state,
                    player,
                    cardRegistry.requireCard("Grizzly Bears")
                ) shouldBe true

                game.execute(CastSpell(player, bears, useWithoutPayingManaCost = true)).error shouldBe null
                game.state.stack.contains(bears) shouldBe true
            }

            test("it neither benefits an opponent nor applies while absent from the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Omniscience")
                    .withCardInHand(2, "Grizzly Bears")
                    .build()

                calculator.hasFreeCastPermission(
                    game.state,
                    game.player2Id,
                    cardRegistry.requireCard("Grizzly Bears")
                ) shouldBe false

                val withoutOmniscience = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grizzly Bears")
                    .build()

                calculator.hasFreeCastPermission(
                    withoutOmniscience.state,
                    withoutOmniscience.player1Id,
                    cardRegistry.requireCard("Grizzly Bears")
                ) shouldBe false
            }

            test("the card uses the unrestricted controller-only static") {
                val ability = cardRegistry.requireCard("Omniscience").script.staticAbilities
                    .filterIsInstance<MayCastWithoutPayingManaCost>()
                    .single()

                ability.controllerOnly shouldBe true
                ability.firstSpellOfTurnOnly shouldBe false
                ability.oncePerTurn shouldBe false
                ability.fromZones shouldBe setOf(Zone.HAND)
            }
        }
    }
}
