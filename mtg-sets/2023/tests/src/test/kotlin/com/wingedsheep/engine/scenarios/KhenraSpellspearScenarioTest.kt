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
 * Khenra Spellspear // Gitaxian Spellstalker (MOM #151).
 *
 *   Front (2/2) — Trample, prowess. "{3}{U/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/3) — Trample, ward {2}, prowess, prowess.
 */
class KhenraSpellspearScenarioTest : ScenarioTestBase() {

    init {
        context("Khenra Spellspear") {

            test("transforms into a 3/3 whose two prowess instances each trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Khenra Spellspear", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(1, "Shock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spear = game.findPermanent("Khenra Spellspear")!!
                val mountains = game.findAllPermanents("Mountain")
                val abilityId = cardRegistry.getCard("Khenra Spellspear")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = spear, abilityId = abilityId,
                        paymentStrategy = PaymentStrategy.Explicit(
                            manaAbilitiesToActivate = mountains.take(3),
                            phyrexianLifePayments = listOf(Color.BLUE)
                        )
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                withClue("transformed into a 3/3 Gitaxian Spellstalker") {
                    game.state.getEntity(spear)!!.get<CardComponent>()!!.name shouldBe "Gitaxian Spellstalker"
                    game.state.projectedState.getPower(spear) shouldBe 3
                    game.state.projectedState.getToughness(spear) shouldBe 3
                }

                val cast = game.castSpellTargetingPlayer(1, "Shock", 2)
                withClue("cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                withClue("two prowess triggers: +2/+2") {
                    game.state.projectedState.getPower(spear) shouldBe 5
                    game.state.projectedState.getToughness(spear) shouldBe 5
                }
            }
        }
    }
}
