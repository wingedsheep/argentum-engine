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
 * Bloatfly Swarm (PIP #42): prevent damage while it has a +1/+1 counter, remove that many
 * counters, then give each player a rad counter per counter removed. The rad counters are the
 * replacement's own rider (`onRemoved`), so they are there as soon as the damage would have been
 * dealt — nothing goes on the stack — and they count the counters removed, not the damage.
 */
class BloatflySwarmScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun seedPlusOnes(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    init {
        context("Bloatfly Swarm") {

            test("enters with five +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bloatfly Swarm")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bloatfly Swarm").error shouldBe null
                game.resolveStack()

                val swarm = game.findPermanent("Bloatfly Swarm")!!
                count(game, swarm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
                game.state.projectedState.getPower(swarm) shouldBe 5
            }

            test("prevented spell damage removes that many counters and gives each player that many rad counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Bloatfly Swarm")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val swarm = game.findPermanent("Bloatfly Swarm")!!
                seedPlusOnes(game, swarm, 5)

                game.castSpell(1, "Shock", swarm).error shouldBe null
                game.resolveStack()

                withClue("2 damage prevented, two counters removed") {
                    count(game, swarm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                    game.isOnBattlefield("Bloatfly Swarm") shouldBe true
                }
                withClue("each player gets a rad counter per counter removed, with nothing on the stack") {
                    count(game, game.player1Id, CounterType.RAD) shouldBe 2
                    count(game, game.player2Id, CounterType.RAD) shouldBe 2
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("damage above the counter count gives rad counters for the counters actually removed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Bloatfly Swarm")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val swarm = game.findPermanent("Bloatfly Swarm")!!
                seedPlusOnes(game, swarm, 2)

                game.castSpell(1, "Lightning Bolt", swarm).error shouldBe null
                game.resolveStack()

                withClue("3 damage, only 2 counters to remove: two rad counters each, not three") {
                    count(game, game.player1Id, CounterType.RAD) shouldBe 2
                    count(game, game.player2Id, CounterType.RAD) shouldBe 2
                }
                withClue("its last counters gone, the 0/0 dies") {
                    game.isOnBattlefield("Bloatfly Swarm") shouldBe false
                }
            }

            test("damage that can't be prevented still removes that many counters and gives the rad counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Bloatfly Swarm")
                    .withCardInHand(1, "Pinpoint Avalanche")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val swarm = game.findPermanent("Bloatfly Swarm")!!
                seedPlusOnes(game, swarm, 5)

                game.castSpell(1, "Pinpoint Avalanche", swarm).error shouldBe null
                game.resolveStack()

                withClue("4 unpreventable damage: four counters removed, four rad counters each (the ruling)") {
                    count(game, game.player1Id, CounterType.RAD) shouldBe 4
                    count(game, game.player2Id, CounterType.RAD) shouldBe 4
                }
                withClue("the damage is dealt to what is now a 1/1, so it dies") {
                    game.isOnBattlefield("Bloatfly Swarm") shouldBe false
                }
            }

            test("combat damage is prevented the same way") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Bloatfly Swarm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val swarm = game.findPermanent("Bloatfly Swarm")!!
                seedPlusOnes(game, swarm, 5)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Bloatfly Swarm" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Bears' 2 damage is prevented: three counters left, two rad counters each") {
                    count(game, swarm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                    count(game, game.player1Id, CounterType.RAD) shouldBe 2
                    count(game, game.player2Id, CounterType.RAD) shouldBe 2
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
