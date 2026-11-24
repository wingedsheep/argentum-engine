package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Cryptborn Horror — "This creature enters with X +1/+1 counters on it, where X is the total life
 * lost by your opponents this turn."
 *
 * The count is read as the Horror enters (a replacement effect), from its controller's *opponents'*
 * life lost this turn: the controller's own life loss doesn't count, and with none it enters as a
 * 0/0 and dies.
 */
class CryptbornHorrorScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Cryptborn Horror")
        .withCardInHand(1, "Lava Spike")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enters with no counters and dies when no opponent lost life this turn") {
            val g = board()
            g.castSpell(1, "Cryptborn Horror").error shouldBe null
            g.resolveStack()
            g.isOnBattlefield("Cryptborn Horror") shouldBe false
            g.isInGraveyard(1, "Cryptborn Horror") shouldBe true
        }

        test("enters with a +1/+1 counter for each life an opponent lost this turn") {
            val g = board()
            g.castSpellTargetingPlayer(1, "Lava Spike", 2).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 17
            g.castSpell(1, "Cryptborn Horror").error shouldBe null
            g.resolveStack()
            val horror = g.findPermanent("Cryptborn Horror")!!
            g.state.getEntity(horror)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }

        test("your own life loss doesn't count") {
            val g = board()
            g.castSpellTargetingPlayer(1, "Lava Spike", 1).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 17
            g.castSpell(1, "Cryptborn Horror").error shouldBe null
            g.resolveStack()
            g.isInGraveyard(1, "Cryptborn Horror") shouldBe true
        }
    }
}
