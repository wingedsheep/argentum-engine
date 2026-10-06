package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rodolf Duskbringer (J22 #25):
 *   Flying, deathtouch, lifelink
 *   Whenever you gain life, Rodolf Duskbringer gains indestructible until end of turn.
 *   At the beginning of your end step, you may pay {1}{W/B}. When you do, return target creature
 *   card with mana value X or less from your graveyard to the battlefield, where X is the amount
 *   of life you gained this turn.
 *
 * Exercises the hybrid optional payment feeding a reflexive trigger whose target cap is the
 * dynamic life-gained-this-turn amount.
 */
class RodolfDuskbringerScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (state.pendingDecision is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        context("Rodolf Duskbringer") {

            test("gaining life grants indestructible until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rodolf Duskbringer")
                    .withCardInHand(1, "Natural Spring")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val rodolf = game.findPermanent("Rodolf Duskbringer")!!
                game.state.projectedState.hasKeyword(rodolf, Keyword.INDESTRUCTIBLE) shouldBe false

                game.castSpellTargetingPlayer(1, "Natural Spring", 1).error shouldBe null
                game.resolveStack()

                withClue("Rodolf is indestructible after its controller gains life") {
                    game.state.projectedState.hasKeyword(rodolf, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }

            test("paying {1}{W/B} returns a creature card with mana value <= life gained") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rodolf Duskbringer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.payIfAsked()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned to the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("a creature card above the cap can't be targeted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rodolf Duskbringer")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(3))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                if (game.state.pendingDecision is com.wingedsheep.engine.core.YesNoDecision) {
                    game.answerYesNo(true)
                    game.payIfAsked()
                }
                if (game.state.pendingDecision is com.wingedsheep.engine.core.ChooseTargetsDecision) {
                    val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                    val decision = game.state.pendingDecision as com.wingedsheep.engine.core.ChooseTargetsDecision
                    withClue("Hill Giant (MV 4) is not a legal target when X = 3") {
                        decision.legalTargets.values.flatten().contains(giant) shouldBe false
                    }
                }
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("declining to pay returns nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rodolf Duskbringer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(5))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
