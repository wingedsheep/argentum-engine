package com.wingedsheep.ai.engine

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * [CombatMath.duel] plays a one-on-one fight out step by step: who dies, as
 * `(attacker dies, blocker dies)`.
 */
class CombatMathDuelTest : ScenarioTestBase() {

    private fun duel(attacker: String, blocker: String): Pair<Boolean, Boolean> {
        val game = scenario().withPlayers()
            .withCardOnBattlefield(1, attacker)
            .withCardOnBattlefield(2, blocker)
            .build()
        val state = game.state
        return CombatMath.duel(state, state.projectedState, game.findPermanent(attacker)!!, game.findPermanent(blocker)!!)
    }

    init {
        test("a plain trade kills both, and a bigger blocker eats the attacker") {
            duel("Grizzly Bears", "Grizzly Bears") shouldBe (true to true)
            duel("Grizzly Bears", "Hill Giant") shouldBe (true to false)
        }

        test("a first striker that kills a deathtouch attacker takes nothing back") {
            duel("Typhoid Rats", "Youthful Knight") shouldBe (true to false)
        }

        test("deathtouch kills a blocker that does not strike first") {
            duel("Typhoid Rats", "Craw Wurm") shouldBe (true to true)
        }

        test("a double striker hits twice: a Raging Redcap kills a Wind Drake, which kills it back") {
            duel("Raging Redcap", "Wind Drake") shouldBe (true to true)
        }

        test("a double striker that kills in the first-strike step takes nothing back") {
            duel("Fencing Ace", "Typhoid Rats") shouldBe (false to true)
        }

        test("two first strikers deal their damage together") {
            duel("Youthful Knight", "Youthful Knight") shouldBe (true to true)
        }
    }
}
