package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.BramblefortFink
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bramblefort Fink (ELD #311) — {8}: This creature has base power and toughness 10/10 until end
 * of turn. Activate only if you control an Oko planeswalker.
 */
class BramblefortFinkScenarioTest : ScenarioTestBase() {

    init {
        context("Bramblefort Fink") {

            test("with an Oko planeswalker, {8} makes it a 10/10 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bramblefort Fink")
                    .withCardOnBattlefield(1, "Oko, Thief of Crowns")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fink = game.findPermanent("Bramblefort Fink")!!
                game.execute(
                    ActivateAbility(game.player1Id, fink, BramblefortFink.activatedAbilities.first().id)
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(fink) shouldBe 10
                game.state.projectedState.getToughness(fink) shouldBe 10
            }

            test("can't be activated without an Oko planeswalker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bramblefort Fink")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fink = game.findPermanent("Bramblefort Fink")!!
                game.execute(
                    ActivateAbility(game.player1Id, fink, BramblefortFink.activatedAbilities.first().id)
                ).error shouldNotBe null
                game.state.projectedState.getPower(fink) shouldBe 2
            }
        }
    }
}
