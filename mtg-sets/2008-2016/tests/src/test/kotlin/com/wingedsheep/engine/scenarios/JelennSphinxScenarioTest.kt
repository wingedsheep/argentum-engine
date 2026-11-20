package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jelenn Sphinx (DGM #77) — {3}{W}{U} 1/5 Creature — Sphinx.
 *
 *   Flying, vigilance
 *   Whenever this creature attacks, other attacking creatures get +1/+1 until end of turn.
 */
class JelennSphinxScenarioTest : ScenarioTestBase() {
    init {
        test("pumps the other attackers, not itself or non-attackers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jelenn Sphinx", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Jelenn Sphinx" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val sphinx = game.findPermanent("Jelenn Sphinx")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            withClue("the other attacker gets +1/+1") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
            }
            withClue("the Sphinx itself is unchanged") {
                projected.getPower(sphinx) shouldBe 1
                projected.getToughness(sphinx) shouldBe 5
            }
            withClue("a non-attacking creature is unchanged") {
                projected.getPower(giant) shouldBe 3
            }
            withClue("vigilance: the Sphinx doesn't tap to attack") {
                game.state.getEntity(sphinx)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
