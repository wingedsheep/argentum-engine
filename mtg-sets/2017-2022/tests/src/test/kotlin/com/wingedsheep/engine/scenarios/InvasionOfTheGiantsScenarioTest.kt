package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of the Giants (KHM #215).
 *
 * Chapter II: draw, then the optional reveal of a Giant card from hand. Revealing one fires a
 * reflexive trigger that targets as it goes on the stack and deals 2 damage; declining deals none.
 * Chapter III: the next Giant spell costs {2} less, and a non-Giant spell cast first doesn't use
 * the reduction up.
 */
class InvasionOfTheGiantsScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Invasion of the Giants")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        fun TestGame.setLore(count: Int) {
            val saga = findPermanent("Invasion of the Giants")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, count))
                    .with(SagaComponent(triggeredChapters = (1..count).toSet()))
            }
        }

        context("chapter II") {
            test("revealing a Giant card fires a targeted reflexive trigger for 2 damage") {
                val game = builder()
                    .withCardInHand(1, "Craven Hulk")
                    .build()
                game.setLore(1)
                val hulk = game.findCardsInHand(1, "Craven Hulk").single()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val handBefore = game.handSize(1)
                game.resolveStack()

                withClue("the card is drawn before the reveal is offered") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(hulk)).error shouldBe null

                withClue("the reflexive trigger chooses its target as it goes on the stack") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.isInHand(1, "Craven Hulk") shouldBe true
            }

            test("declining the reveal deals no damage") {
                val game = builder()
                    .withCardInHand(1, "Craven Hulk")
                    .build()
                game.setLore(1)

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("no reflexive trigger, no damage") {
                    game.getPendingDecision() shouldBe null
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("chapter III") {
            test("the next Giant spell costs {2} less; a non-Giant spell doesn't consume it") {
                val game = builder()
                    .withCardInHand(1, "Craven Hulk")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .build()
                game.setLore(2)

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()
                game.state.spellCostModifiers.size shouldBe 1

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                withClue("Shock isn't a Giant, so the reduction is still waiting") {
                    game.state.spellCostModifiers.size shouldBe 1
                }

                withClue("Craven Hulk ({3}{R}) is cast with the two remaining Mountains") {
                    game.castSpell(1, "Craven Hulk").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Craven Hulk") shouldBe true
                game.state.spellCostModifiers shouldBe emptyList()
            }
        }
    }
}
