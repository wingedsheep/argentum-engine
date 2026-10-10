package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Halo Fountain (SNC) — three abilities whose costs untap tapped creatures you control.
 *
 * The cost mechanics themselves are pinned in `UntapPermanentsCostTest`; this covers the card:
 * each ability is offered only with enough tapped creatures, and the fifteen-creature ability
 * wins the game.
 */
class HaloFountainScenarioTest : ScenarioTestBase() {

    private fun TestGame.activate(index: Int, chosen: List<EntityId>) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Halo Fountain")!!,
            abilityId = cardRegistry.getCard("Halo Fountain")!!.activatedAbilities[index].id,
            costPayment = AdditionalCostPayment(tappedPermanents = chosen)
        )
    )

    private fun TestGame.fountainAbilityIds() = getLegalActions(1)
        .filter { it.isAffordable }
        .mapNotNull { it.action as? ActivateAbility }
        .filter { it.sourceId == findPermanent("Halo Fountain") }
        .map { it.abilityId }
        .toSet()

    init {
        context("Halo Fountain") {
            test("untapping a creature makes a 1/1 green and white Citizen") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Halo Fountain")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val abilities = cardRegistry.getCard("Halo Fountain")!!.activatedAbilities
                withClue("one tapped creature affords only the Citizen ability") {
                    game.fountainAbilityIds() shouldBe setOf(abilities[0].id)
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.activate(0, listOf(bears)).error shouldBe null
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                game.resolveStack()

                game.findPermanents("Citizen Token").size shouldBe 1
            }

            test("untapping fifteen creatures wins the game") {
                val builder = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Halo Fountain")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(15) { builder.withCardOnBattlefield(1, "Grizzly Bears", tapped = true) }
                val game = builder.build()

                val bears = game.findPermanents("Grizzly Bears")
                game.activate(2, bears).error shouldBe null
                withClue("every creature was untapped to pay the cost") {
                    bears.none { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }
        }
    }
}
