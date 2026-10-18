package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hidetsugu Consumes All // Vessel of the All-Consuming (NEO #221).
 *
 * Chapter I sweeps nonland permanents with mana value 1 or less. The Vessel grows once per
 * damage event — trampling over a blocker is one trigger — and a player it has dealt 10 or more
 * damage to this turn loses the game.
 */
class HidetsuguConsumesAllScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.plusOnes(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.addPlusOnes(id: EntityId, n: Int) {
            state = state.updateEntity(id) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
            }
        }

        fun vesselGame(blocker: String? = null) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vessel of the All-Consuming")
            .apply { if (blocker != null) withCardOnBattlefield(2, blocker) }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Hidetsugu Consumes All") {

            test("chapter I destroys each nonland permanent with mana value 1 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hidetsugu Consumes All")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Memnite")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hidetsugu Consumes All").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Memnite") shouldBe true
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                withClue("mana value 2 survives, as do lands and the Saga itself") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    game.findPermanents("Swamp").size shouldBe 2
                    game.findPermanent("Hidetsugu Consumes All") shouldNotBe null
                }
            }
        }

        context("Vessel of the All-Consuming") {

            test("trampling over a blocker puts a single +1/+1 counter on it") {
                val game = vesselGame(blocker = "Llanowar Elves")
                val vessel = game.findPermanent("Vessel of the All-Consuming")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Vessel of the All-Consuming" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Llanowar Elves" to listOf("Vessel of the All-Consuming"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("1 to the Elves and 2 trampling over is one damage event") {
                    game.plusOnes(vessel) shouldBe 1
                }
                game.getLifeTotal(2) shouldBe 18
            }

            test("a player it has dealt 10 damage to this turn loses the game") {
                val game = vesselGame()
                val vessel = game.findPermanent("Vessel of the All-Consuming")!!
                game.addPlusOnes(vessel, 7)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Vessel of the All-Consuming" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                var guard = 0
                while (!game.state.gameOver && guard++ < 30) {
                    if (game.state.pendingDecision != null) game.submitDefaultCombatDamage() else game.passPriority()
                }

                withClue("10 damage leaves 10 life, so the loss is the trigger's") {
                    game.getLifeTotal(2) shouldBe 10
                }
                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }

            test("9 damage is not enough") {
                val game = vesselGame()
                val vessel = game.findPermanent("Vessel of the All-Consuming")!!
                game.addPlusOnes(vessel, 6)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Vessel of the All-Consuming" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 11
                game.state.gameOver shouldBe false
                game.plusOnes(vessel) shouldBe 7
            }
        }
    }
}
