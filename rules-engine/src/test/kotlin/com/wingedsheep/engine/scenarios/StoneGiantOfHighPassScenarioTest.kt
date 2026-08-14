package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.StoneGiantOfHighPass
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stone-Giant of High Pass (HOB) — {5}{R}{R} Creature — Giant 7/7.
 *
 * "Whenever this creature enters or attacks, create a 3/1 colorless Wall artifact creature token
 *  with defender named Stone Boulder.
 *  {2}{R}, Sacrifice an artifact: This creature deals 4 damage to any target."
 *
 * Enters and attacks are two separate triggers, so a Giant that enters and then attacks in the same
 * turn ends up with two Boulders. The Boulders are artifacts, which is what feeds the sacrifice cost
 * on the damage ability.
 */
class StoneGiantOfHighPassScenarioTest : ScenarioTestBase() {

    init {
        context("Stone-Giant of High Pass") {

            test("it is a 7/7") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stone-Giant of High Pass")
                    .build()

                val giant = game.findPermanent("Stone-Giant of High Pass")!!
                game.state.projectedState.getPower(giant) shouldBe 7
                game.state.projectedState.getToughness(giant) shouldBe 7
            }

            test("entering creates a 3/1 Stone Boulder with defender") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stone-Giant of High Pass")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stone-Giant of High Pass").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val boulder = game.findPermanent("Stone Boulder")
                    ?: error("the enters trigger should have created a Stone Boulder")
                withClue("a 3/1 artifact creature with defender") {
                    game.state.projectedState.getPower(boulder) shouldBe 3
                    game.state.projectedState.getToughness(boulder) shouldBe 1
                    game.state.projectedState.hasKeyword(boulder, Keyword.DEFENDER) shouldBe true
                }
            }

            test("attacking creates another Boulder") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stone-Giant of High Pass", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the board starts clean — this Giant was put into play, not cast") {
                    game.findAllPermanents("Stone Boulder").size shouldBe 0
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Stone-Giant of High Pass" to 2)).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Stone Boulder").size shouldBe 1
            }

            test("{2}{R}, sacrificing the Boulder deals 4 damage to any target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stone-Giant of High Pass")
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stone-Giant of High Pass").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val giant = game.findPermanent("Stone-Giant of High Pass")!!
                val boulder = game.findPermanent("Stone Boulder")!!
                val ability = StoneGiantOfHighPass.activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = giant,
                        abilityId = ability,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                ).error shouldBe null

                var guard = 0
                while (guard++ < 10) {
                    when (game.getPendingDecision()) {
                        is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                        is SelectCardsDecision -> game.selectCards(listOf(boulder))
                        else -> break
                    }
                }
                game.resolveStack()

                withClue("4 damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("the Boulder was sacrificed to pay the cost") {
                    game.findAllPermanents("Stone Boulder").size shouldBe 0
                }
            }
        }
    }
}
