package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Urborg Repossession — Dominaria United #114, {B} Sorcery, Kicker {1}{G}
 *
 * "Return target creature card from your graveyard to your hand. You gain 2 life. If this spell
 *  was kicked, return another target permanent card from your graveyard to your hand."
 *
 * The kicked cast announces a second, *different* target (CR 601.2c); the plain cast has one.
 */
class UrborgRepossessionScenarioTest : ScenarioTestBase() {

    init {
        context("Urborg Repossession") {

            fun game() = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Urborg Repossession")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("unkicked, it returns the creature card and gains 2 life") {
                val game = game()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val spell = game.findCardsInHand(1, "Urborg Repossession").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.getLifeTotal(1) shouldBe 22
            }

            test("kicked, it also returns another permanent card") {
                val game = game()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val forest = game.findCardsInGraveyard(1, "Forest").single()
                val spell = game.findCardsInHand(1, "Urborg Repossession").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(forest, game.player1Id, Zone.GRAVEYARD),
                        ),
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                game.getLifeTotal(1) shouldBe 22
            }

            test("kicked, the second target can't be the same creature card") {
                val game = game()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val spell = game.findCardsInHand(1, "Urborg Repossession").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                        ),
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error.shouldNotBeNull()
                game.isInHand(1, "Urborg Repossession") shouldBe true
            }
        }
    }
}
