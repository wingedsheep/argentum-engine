package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Paladin of Atonement (RIX #16) — at the beginning of each upkeep, if you lost life last turn, put a
 * +1/+1 counter on it; when it dies, you gain life equal to its toughness.
 */
class PaladinOfAtonementScenarioTest : ScenarioTestBase() {
    init {
        fun paladinGame() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Paladin of Atonement")
            .withCardInHand(1, "Shock")
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
            state.getEntity(findPermanent("Paladin of Atonement")!!)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("losing life grows it at the next upkeep, even the opponent's") {
            val game = paladinGame()
            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.resolveStack()

            game.plusOnes() shouldBe 1
        }

        test("an opponent's life loss doesn't count") {
            val game = paladinGame()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.stack shouldBe emptyList()
            game.resolveStack()

            game.plusOnes() shouldBe 0
        }

        test("dying gains life equal to its last toughness, counters included") {
            val game = paladinGame()
            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 18

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.plusOnes() shouldBe 1

            // Nobody loses life on the opponent's turn, so the Paladin stays 2/2 into our main phase.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.stack shouldBe emptyList()
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.plusOnes() shouldBe 1

            game.castSpell(1, "Shock", game.findPermanent("Paladin of Atonement")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Paladin of Atonement") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
