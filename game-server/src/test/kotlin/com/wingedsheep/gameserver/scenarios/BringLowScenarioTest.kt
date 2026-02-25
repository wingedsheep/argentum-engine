package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class BringLowScenarioTest : ScenarioTestBase() {

    init {
        context("Bring Low - conditional damage based on +1/+1 counters") {
            test("deals 3 damage to creature without +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bring Low")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Towering Baloth")!!
                game.castSpell(1, "Bring Low", targetId)
                game.resolveStack()

                withClue("Towering Baloth should survive with 3 toughness remaining") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Towering Baloth should have 3 toughness (6 - 3 damage)") {
                    cardInfo!!.toughness shouldBe 6
                    cardInfo.damage shouldBe 3
                }
            }

            test("deals 5 damage to creature with a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bring Low")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Towering Baloth")!!

                // Add a +1/+1 counter to the target
                val counters = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                game.state = game.state.updateEntity(targetId) { c -> c.with(counters) }

                game.castSpell(1, "Bring Low", targetId)
                game.resolveStack()

                withClue("Towering Baloth should survive but with 5 damage (toughness is 7 with counter)") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Towering Baloth should have taken 5 damage") {
                    cardInfo!!.damage shouldBe 5
                }
            }

            test("deals 3 damage killing a 3-toughness creature without counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bring Low")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Elvish Warrior")!!
                game.castSpell(1, "Bring Low", targetId)
                game.resolveStack()

                withClue("Elvish Warrior should be dead from 3 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
            }

            test("deals 5 damage killing a 4-toughness creature with a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bring Low")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Hill Giant")!!

                // Add a +1/+1 counter — Hill Giant becomes 4/4
                val counters = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                game.state = game.state.updateEntity(targetId) { c -> c.with(counters) }

                game.castSpell(1, "Bring Low", targetId)
                game.resolveStack()

                withClue("Hill Giant (4/4 with counter) should be dead from 5 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }
        }
    }
}
