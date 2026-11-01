package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gideon, the Oathsworn (War of the Spark #265).
 *
 *   Whenever you attack with two or more non-Gideon creatures, put a +1/+1 counter on each of
 *   those creatures.
 *
 * The loyalty abilities reuse the Gideon, Champion of Justice animate/prevent shape and a plain
 * exile; this covers the attack trigger: the count threshold over a filtered attacker set, and the
 * counters going on exactly those attackers (never on an attacking Gideon, per the rulings).
 */
class GideonTheOathswornScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("attacking with two non-Gideon creatures puts a +1/+1 counter on each of them") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gideon, the Oathsworn")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.resolveStack()

            game.plusOnes("Grizzly Bears") shouldBe 1
            game.plusOnes("Hill Giant") shouldBe 1
            withClue("a creature that didn't attack gets nothing") { game.plusOnes("Savannah Lions") shouldBe 0 }
        }

        test("a single attacker doesn't trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gideon, the Oathsworn")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.plusOnes("Grizzly Bears") shouldBe 0
        }

        test("ruling: an attacking Gideon neither counts toward the two nor gets a counter") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gideon, the Oathsworn")
                // A 4/4 Gideon creature-planeswalker during its controller's turn.
                .withCardOnBattlefield(1, "Gideon Blackblade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Gideon Blackblade" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            withClue("only one non-Gideon attacker: no trigger") { game.plusOnes("Grizzly Bears") shouldBe 0 }
            game.plusOnes("Gideon Blackblade") shouldBe 0
        }

        test("ruling: with two non-Gideon attackers, they get counters but the attacking Gideon doesn't") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Gideon, the Oathsworn")
                .withCardOnBattlefield(1, "Gideon Blackblade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Gideon Blackblade" to 2, "Grizzly Bears" to 2, "Hill Giant" to 2)
            ).error shouldBe null
            game.resolveStack()

            game.plusOnes("Grizzly Bears") shouldBe 1
            game.plusOnes("Hill Giant") shouldBe 1
            game.plusOnes("Gideon Blackblade") shouldBe 0
        }
    }
}
