package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.RetributiveWand
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Retributive Wand (M20 #236) — {3} Artifact.
 *
 *   {3}, {T}: This artifact deals 1 damage to any target.
 *   When this artifact is put into a graveyard from the battlefield, it deals 5 damage to any target.
 */
class RetributiveWandScenarioTest : ScenarioTestBase() {

    init {
        context("Retributive Wand") {

            test("{3}, {T} deals 1 damage to any target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retributive Wand")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wand = game.findPermanent("Retributive Wand")!!
                game.execute(
                    ActivateAbility(
                        game.player1Id,
                        wand,
                        RetributiveWand.activatedAbilities.first().id,
                        listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
            }

            test("going to the graveyard from the battlefield deals 5 damage to any target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retributive Wand")
                    .withCardInHand(2, "Shatter")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wand = game.findPermanent("Retributive Wand")!!
                game.castSpell(2, "Shatter", wand).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Retributive Wand") shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.playerId shouldBe game.player1Id
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15
            }
        }
    }
}
