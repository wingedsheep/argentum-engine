package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Havengul Vampire (AVR #139) — {3}{R} Creature — Vampire 2/2
 *
 *   Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *   Whenever another creature dies, put a +1/+1 counter on this creature.
 */
class HavengulVampireScenarioTest : ScenarioTestBase() {

    init {
        fun counters(game: TestGame): Int =
            game.state.getEntity(game.findPermanent("Havengul Vampire")!!)
                ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("unblocked combat damage to a player puts a +1/+1 counter on it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Havengul Vampire")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Havengul Vampire" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            withClue("one counter after hitting the player") { counters(game) shouldBe 1 }
        }

        test("another creature dying — either player's — puts a +1/+1 counter on it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Havengul Vampire")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            withClue("own creature died") { counters(game) shouldBe 1 }

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            withClue("opponent's creature died") { counters(game) shouldBe 2 }
        }
    }
}
