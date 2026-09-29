package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Serpent-Blade Assailant (MOM #205) — {2}{G} 2/1. Backup 1; deathtouch.
 */
class SerpentBladeAssailantScenarioTest : ScenarioTestBase() {
    init {
        context("Serpent-Blade Assailant") {

            test("backup on another creature gives a +1/+1 counter and deathtouch until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Serpent-Blade Assailant")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Serpent-Blade Assailant").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(bears)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
            }

            test("backup on itself just puts the counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Serpent-Blade Assailant")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Serpent-Blade Assailant").error shouldBe null
                game.resolveStack()
                val self = game.findPermanent("Serpent-Blade Assailant")!!
                if (game.hasPendingDecision()) game.selectTargets(listOf(self)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(self)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.hasKeyword(self, Keyword.DEATHTOUCH) shouldBe true
            }
        }
    }
}
