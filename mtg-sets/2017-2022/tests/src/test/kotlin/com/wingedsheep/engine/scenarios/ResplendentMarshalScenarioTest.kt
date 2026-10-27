package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Resplendent Marshal: the exiled card is read back by the reflexive payoff
 * through the pipeline, the Marshal itself never gets a counter, and on death it can't exile itself.
 */
class ResplendentMarshalScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Resplendent Marshal") {
            test("enters: exiling a Human Warrior counters only other creatures sharing a type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Resplendent Marshal")
                    .withCardInGraveyard(1, "Kjeldoran Warrior") // Human Warrior
                    .withCardOnBattlefield(1, "Elite Vanguard") // Human Soldier — shares Human
                    .withCardOnBattlefield(1, "Grizzly Bears") // Bear — shares nothing
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Resplendent Marshal").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger offers the optional exile") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Kjeldoran Warrior was exiled") {
                    game.findCardsInGraveyard(1, "Kjeldoran Warrior").size shouldBe 0
                }
                val marshal = game.findPermanent("Resplendent Marshal")!!
                val vanguard = game.findPermanent("Elite Vanguard")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Elite Vanguard shares Human with the exiled card") { game.plusOnes(vanguard) shouldBe 1 }
                withClue("Grizzly Bears shares no creature type") { game.plusOnes(bears) shouldBe 0 }
                withClue("the Marshal shares Warrior but is excluded") { game.plusOnes(marshal) shouldBe 0 }
            }

            test("dies: can't exile itself, exiles the other creature card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Resplendent Marshal")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elite Vanguard")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshal = game.findPermanent("Resplendent Marshal")!!
                game.castSpell(1, "Murder", marshal).error shouldBe null
                game.resolveStack()

                withClue("the dies trigger offers the optional exile") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the Marshal stays in the graveyard; the Bears card was exiled") {
                    game.findCardsInGraveyard(1, "Resplendent Marshal").size shouldBe 1
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("the Bears on the battlefield shares Bear") {
                    game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 1
                }
                withClue("Elite Vanguard shares nothing with a Bear") {
                    game.plusOnes(game.findPermanent("Elite Vanguard")!!) shouldBe 0
                }
            }

            test("dies with no other creature card in the graveyard: no prompt") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Resplendent Marshal")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Resplendent Marshal")!!).error shouldBe null
                game.resolveStack()

                withClue("only the Marshal itself is there, so the may-question is skipped") {
                    game.hasPendingDecision() shouldBe false
                }
                game.findCardsInGraveyard(1, "Resplendent Marshal").size shouldBe 1
            }
        }
    }
}
