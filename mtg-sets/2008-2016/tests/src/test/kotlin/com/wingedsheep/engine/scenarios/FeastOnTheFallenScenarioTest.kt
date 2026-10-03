package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feast on the Fallen (M15 #96) — at the beginning of each upkeep, if an opponent lost life last
 * turn, put a +1/+1 counter on target creature you control.
 */
class FeastOnTheFallenScenarioTest : ScenarioTestBase() {
    init {
        fun feastGame() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Feast on the Fallen")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Shock")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.plusOnes(): Int =
            state.getEntity(findPermanent("Grizzly Bears")!!)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("an opponent losing life this turn grows a creature at the next upkeep — the opponent's") {
            val game = feastGame()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the trigger asks for a creature you control") {
                game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            }
            game.resolveStack()

            game.plusOnes() shouldBe 1
        }

        test("your own life loss doesn't count") {
            val game = feastGame()
            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.stack shouldBe emptyList()
            game.resolveStack()

            game.plusOnes() shouldBe 0
        }

        test("the record covers the previous turn only") {
            val game = feastGame()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.plusOnes() shouldBe 1

            // Nobody loses life during the opponent's turn, so the upkeep after it doesn't trigger.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.stack shouldBe emptyList()
            game.resolveStack()

            game.plusOnes() shouldBe 1
        }
    }
}
