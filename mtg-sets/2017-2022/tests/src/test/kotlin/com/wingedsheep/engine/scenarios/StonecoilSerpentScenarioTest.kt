package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stonecoil Serpent (ELD #235) — {X} Artifact Creature — Snake 0/0.
 *
 * "Reach, trample, protection from multicolored. This creature enters with X +1/+1 counters on it."
 */
class StonecoilSerpentScenarioTest : ScenarioTestBase() {

    init {
        context("Stonecoil Serpent") {

            test("X = 3 enters with three +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stonecoil Serpent")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val r = game.castXSpell(1, "Stonecoil Serpent", 3)
                withClue("${r.error}") { r.error shouldBe null }
                game.resolveStack()

                val serpent = game.findPermanent("Stonecoil Serpent")!!
                game.state.getEntity(serpent)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                game.state.projectedState.getPower(serpent) shouldBe 3
                game.state.projectedState.getToughness(serpent) shouldBe 3
            }

            test("protection from multicolored: a multicolored spell can't target it, a monocolored one can") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stonecoil Serpent")
                    .withCardInHand(1, "Lightning Helix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val serpent = game.findPermanent("Stonecoil Serpent")!!
                game.castSpell(1, "Lightning Helix", serpent).error shouldNotBe null
                game.castSpell(1, "Lightning Bolt", serpent).error shouldBe null
            }
        }
    }
}
