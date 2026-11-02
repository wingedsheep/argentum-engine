package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Brad Boimler, Eager Ensign: "Whenever Brad Boimler becomes tapped, until end of turn, if one or more
 * counters would be put on a permanent you control, that many plus one of each of those kinds of
 * counters are put on that permanent instead."
 */
class BradBoimlerEagerEnsignScenarioTest : ScenarioTestBase() {

    /** Puts two kinds of counters at once, so "plus one of each of those kinds" is observable. */
    private val boost = card("Boimler Test Boost") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Put two +1/+1 counters and a lore counter on target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
                Effects.AddCounters(CounterType.LORE, 1, creature)
        }
    }

    private fun TestGame.counters(name: String, kind: CounterType): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(kind) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Brad Boimler, Eager Ensign")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(1, "Boimler Test Boost")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        cardRegistry.register(boost)

        context("Brad Boimler, Eager Ensign") {
            test("untapped, counters go on unchanged") {
                val game = board().build()
                game.castSpell(1, "Boimler Test Boost", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.counters("Grizzly Bears", CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.counters("Grizzly Bears", CounterType.LORE) shouldBe 1
            }

            test("after he becomes tapped, each kind put on a permanent you control gets one more") {
                val game = board().build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Brad Boimler, Eager Ensign" to 2)).error shouldBe null
                game.resolveStack() // the becomes-tapped trigger installs the modifier

                game.castSpell(1, "Boimler Test Boost", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                withClue("two +1/+1 become three, one lore becomes two") {
                    game.counters("Grizzly Bears", CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                    game.counters("Grizzly Bears", CounterType.LORE) shouldBe 2
                }
            }

            test("a permanent an opponent controls is unaffected") {
                val game = board().build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Brad Boimler, Eager Ensign" to 2)).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Boimler Test Boost", game.findPermanent("Hill Giant")).error shouldBe null
                game.resolveStack()

                game.counters("Hill Giant", CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.counters("Hill Giant", CounterType.LORE) shouldBe 1
            }
        }
    }
}
