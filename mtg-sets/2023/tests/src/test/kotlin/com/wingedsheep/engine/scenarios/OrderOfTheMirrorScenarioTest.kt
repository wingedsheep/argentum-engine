package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Order of the Mirror // Order of the Alabaster Host (MOM #72).
 *
 *   Front (2/1) — "{3}{W/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/3) — "Whenever this creature becomes blocked by a creature, the blocking creature gets -1/-1
 *                  until end of turn."
 */
class OrderOfTheMirrorScenarioTest : ScenarioTestBase() {

    init {
        context("Order of the Mirror") {

            test("paying {W/P} with 2 life transforms it into a 3/3 Order of the Alabaster Host") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Order of the Mirror")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val order = game.findPermanent("Order of the Mirror")!!
                val islands = game.findAllPermanents("Island")
                val abilityId = cardRegistry.getCard("Order of the Mirror")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = order, abilityId = abilityId,
                        paymentStrategy = PaymentStrategy.Explicit(
                            manaAbilitiesToActivate = islands,
                            phyrexianLifePayments = listOf(Color.WHITE)
                        )
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                game.state.getEntity(order)!!.get<CardComponent>()!!.name shouldBe "Order of the Alabaster Host"
                game.state.projectedState.getPower(order) shouldBe 3
                game.state.projectedState.getToughness(order) shouldBe 3
            }

            test("the transformed face shrinks a blocking creature by -1/-1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Order of the Alabaster Host", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val host = game.findPermanent("Order of the Alabaster Host")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Order of the Alabaster Host" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Order of the Alabaster Host"))).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (3/3) gets -1/-1 when it blocks") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
            }
        }
    }
}
