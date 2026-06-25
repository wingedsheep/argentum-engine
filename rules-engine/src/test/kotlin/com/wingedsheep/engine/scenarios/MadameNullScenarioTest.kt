package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Madame Null, Power Broker (TMT) — the proving card for the dynamic
 * pay-life cost (`Effects.PayDynamicLife`).
 *
 * "Whenever another creature you control enters, you may pay life equal to its power. If you
 *  do, put that many +1/+1 counters on it."
 *
 * Exercises the `OptionalCostEffect(PayDynamicLife(power), AddDynamicCounters(power))` gate:
 *  - pay → life drops by the entering creature's power and it gets that many +1/+1 counters,
 *  - decline → no life paid, no counters,
 *  - a 0-power creature → "pay 0 life" is a no-op payment (CR 119.4) that still adds 0 counters.
 */
class MadameNullScenarioTest : ScenarioTestBase() {

    init {
        context("Madame Null, Power Broker") {

            test("paying life equal to the entering creature's power adds that many +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Madame Null, Power Broker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(1, "Centaur Courser") // {2}{G} 3/3
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Centaur Courser")
                game.resolveStack() // Centaur enters; Madame Null's trigger resolves to the may-pay prompt
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Paid 3 life (Centaur Courser's power)") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("Centaur Courser gained 3 +1/+1 counters") {
                    game.plusOneCounters(game.findOnBattlefield("Centaur Courser")) shouldBe 3
                }
            }

            test("declining the payment adds no counters and costs no life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Madame Null, Power Broker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(1, "Centaur Courser")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Centaur Courser")
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("No life paid when declined") { game.getLifeTotal(1) shouldBe 20 }
                withClue("No counters when declined") {
                    game.plusOneCounters(game.findOnBattlefield("Centaur Courser")) shouldBe 0
                }
            }

            test("a 0-power creature: paying 0 life is a no-op and adds no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Madame Null, Power Broker")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Taunting Elf") // {G} 0/1
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Taunting Elf")
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Paying life equal to 0 power costs no life") { game.getLifeTotal(1) shouldBe 20 }
                withClue("0 power → 0 +1/+1 counters") {
                    game.plusOneCounters(game.findOnBattlefield("Taunting Elf")) shouldBe 0
                }
            }
        }
    }

    private fun TestGame.findOnBattlefield(cardName: String): EntityId =
        state.getBattlefield().first { state.getEntity(it)?.get<CardComponent>()?.name == cardName }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
}
