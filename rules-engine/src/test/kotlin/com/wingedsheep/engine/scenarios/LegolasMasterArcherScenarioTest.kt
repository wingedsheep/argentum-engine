package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Legolas, Master Archer —
 *  - "Whenever you cast a spell that targets Legolas, put a +1/+1 counter on Legolas."
 *  - "Whenever you cast a spell that targets a creature you don't control, Legolas deals
 *     damage equal to its power to up to one target creature."
 *
 * Exercises the new SpellCastPredicate.TargetsSource / TargetsMatching cast-time predicates.
 */
class LegolasMasterArcherScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("a spell targeting Legolas puts a +1/+1 counter on it") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Giant Growth")
                .withCardOnBattlefield(1, "Legolas, Master Archer")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val legolas = game.findPermanent("Legolas, Master Archer")!!
            plusCounters(game, legolas) shouldBe 0

            game.castSpell(1, "Giant Growth", legolas).error shouldBe null
            game.resolveStack()

            plusCounters(game, legolas) shouldBe 1
        }

        test("a spell targeting an opponent's creature makes Legolas deal its power in damage") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Giant Growth")
                .withCardOnBattlefield(1, "Legolas, Master Archer") // 1/4
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")  // the spell's target
                .withCardOnBattlefield(2, "Raging Goblin")  // 1/1 — Legolas's damage target
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val goblin = game.findPermanent("Raging Goblin")!!

            // Cast Giant Growth on the opponent's Grizzly Bears.
            game.castSpell(1, "Giant Growth", bears).error shouldBe null

            // Legolas's trigger asks for up to one target creature — aim it at the 1/1 Goblin.
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(goblin))
            }
            game.resolveStack()

            // Legolas (power 1) dealt 1 damage to the 1/1 Goblin — it died.
            game.findPermanent("Raging Goblin") shouldBe null
            // The Bears survived (Giant Growth only pumped it).
            game.findPermanent("Grizzly Bears") shouldBe bears
        }
    }
}
