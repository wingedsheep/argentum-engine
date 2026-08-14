package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Territorial Witchstalker — {1}{G} Creature — Wolf 2/3 (WOE).
 *
 * Defender
 * At the beginning of combat on your turn, if you control a creature with power 4 or greater,
 * this creature gets +1/+0 until end of turn and can attack this turn as though it didn't have
 * defender.
 *
 * Covers the intervening-if gate (CR 603.4) in both directions and the fact that the granted
 * permission actually lets a defender attack.
 */
class TerritorialWitchstalkerScenarioTest : ScenarioTestBase() {

    private fun game(bigCreature: String?) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Territorial Witchstalker")
        .also { b -> bigCreature?.let { b.withCardOnBattlefield(1, it) } }
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withTurnNumber(3)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .build()

    init {
        test("with a power 4+ creature: gets +1/+0 and can attack despite defender") {
            // Force of Nature is a 5/5, so the intervening 'if' is satisfied.
            val g = game("Force of Nature")
            val wolf = g.findPermanent("Territorial Witchstalker")!!

            g.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            g.resolveStack()

            StateProjector().project(g.state).getPower(wolf) shouldBe 3

            g.declareAttackers(mapOf("Territorial Witchstalker" to 2)).error shouldBe null
        }

        test("without a power 4+ creature: no pump and defender still stops it attacking") {
            val g = game(null)
            val wolf = g.findPermanent("Territorial Witchstalker")!!

            g.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            g.resolveStack()

            StateProjector().project(g.state).getPower(wolf) shouldBe 2

            g.declareAttackers(mapOf("Territorial Witchstalker" to 2)).error shouldNotBe null
        }
    }
}
