package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lumbering Megasloth (PIP #80) {10}{G}{G} 8/8 — "This spell costs {1} less to cast for each
 * counter among players and permanents. Trample. This creature enters tapped."
 *
 * Every counter counts: any kind, on any player or any permanent, whoever controls it. Only the
 * generic part shrinks, so {G}{G} is always owed.
 */
class LumberingMegaslothScenarioTest : ScenarioTestBase() {

    private fun TestGame.addCounters(id: EntityId, vararg kinds: Pair<CounterType, Int>) {
        state = state.updateEntity(id) { it.with(CountersComponent(mapOf(*kinds))) }
    }

    init {
        fun game(lands: Int) = scenario()
            .withPlayers()
            .withCardInHand(1, "Lumbering Megasloth")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withLandsOnBattlefield(1, "Forest", lands)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        // Six counters across both players and both sides' permanents, four different kinds.
        fun TestGame.spreadSixCounters() {
            addCounters(player1Id, CounterType.ENERGY to 2)
            addCounters(player2Id, CounterType.POISON to 1)
            addCounters(findPermanent("Grizzly Bears")!!, CounterType.PLUS_ONE_PLUS_ONE to 2)
            addCounters(findPermanent("Hill Giant")!!, CounterType.MINUS_ONE_MINUS_ONE to 1)
        }

        test("each counter among players and permanents takes {1} off, and it enters tapped with trample") {
            val game = game(lands = 6)
            game.spreadSixCounters()

            val cast = game.castSpell(1, "Lumbering Megasloth")
            withClue("six counters make {10}{G}{G} cost {4}{G}{G}: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val sloth = game.findPermanent("Lumbering Megasloth")
            sloth shouldNotBe null
            game.state.getEntity(sloth!!)?.has<TappedComponent>() shouldBe true
            game.state.projectedState.hasKeyword(sloth, Keyword.TRAMPLE) shouldBe true
        }

        test("one counter short of the reduction it needs, it can't be cast") {
            val game = game(lands = 5)
            game.spreadSixCounters()
            withClue("five lands can't pay {4}{G}{G}") {
                game.castSpell(1, "Lumbering Megasloth").error shouldNotBe null
            }
        }

        test("with no counters anywhere it costs the full twelve") {
            val game = game(lands = 11)
            game.castSpell(1, "Lumbering Megasloth").error shouldNotBe null
        }

        test("the reduction never touches {G}{G}") {
            val game = game(lands = 2)
            game.addCounters(game.player2Id, CounterType.POISON to 9)
            game.addCounters(game.findPermanent("Hill Giant")!!, CounterType.PLUS_ONE_PLUS_ONE to 9)

            val cast = game.castSpell(1, "Lumbering Megasloth")
            withClue("eighteen counters still leave {G}{G}: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            game.isOnBattlefield("Lumbering Megasloth") shouldBe true
        }
    }
}
