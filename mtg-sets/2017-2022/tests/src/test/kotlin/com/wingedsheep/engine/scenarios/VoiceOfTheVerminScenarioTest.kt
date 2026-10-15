package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Voice of the Vermin (SNC #163) — {3}{G} 2/2 Creature — Human Citizen.
 *
 * "Whenever this creature attacks, target creature you control has base power and toughness 4/4
 *  until end of turn."
 */
class VoiceOfTheVerminScenarioTest : ScenarioTestBase() {

    init {
        context("Voice of the Vermin") {
            test("attacking sets a creature you control to base 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Voice of the Vermin")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Voice of the Vermin" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears has base power and toughness 4/4") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
