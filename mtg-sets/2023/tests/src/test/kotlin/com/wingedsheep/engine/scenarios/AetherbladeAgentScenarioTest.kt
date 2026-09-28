package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aetherblade Agent // Gitaxian Mindstinger (MOM #88).
 *
 *   Front (1/1) — Deathtouch. "{4}{U/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/3) — Deathtouch. Whenever this creature deals combat damage to a player or battle, draw a card.
 */
class AetherbladeAgentScenarioTest : ScenarioTestBase() {

    init {
        context("Aetherblade Agent") {

            test("paying {U/P} with 2 life transforms it into a 3/3 Gitaxian Mindstinger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aetherblade Agent")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val agent = game.findPermanent("Aetherblade Agent")!!
                game.state.projectedState.hasKeyword(agent, Keyword.DEATHTOUCH) shouldBe true
                val swamps = game.findAllPermanents("Swamp")
                val abilityId = cardRegistry.getCard("Aetherblade Agent")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = agent, abilityId = abilityId,
                        paymentStrategy = PaymentStrategy.Explicit(
                            manaAbilitiesToActivate = swamps,
                            phyrexianLifePayments = listOf(Color.BLUE)
                        )
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                withClue("transformed into a 3/3 deathtouch Gitaxian Mindstinger") {
                    game.state.getEntity(agent)!!.get<CardComponent>()!!.name shouldBe "Gitaxian Mindstinger"
                    game.state.projectedState.getPower(agent) shouldBe 3
                    game.state.projectedState.getToughness(agent) shouldBe 3
                    game.state.projectedState.hasKeyword(agent, Keyword.DEATHTOUCH) shouldBe true
                }
            }
        }
    }
}
