package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wingshield Agent (SNC #64) — {2}{U} 2/3 Creature — Human Soldier.
 *
 * "This creature enters with a shield counter on it.
 *  Whenever this creature attacks, up to one other target creature gains flying until end of turn."
 */
class WingshieldAgentScenarioTest : ScenarioTestBase() {

    init {
        context("Wingshield Agent") {
            test("enters with a shield counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wingshield Agent")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wingshield Agent").error shouldBe null
                game.resolveStack()

                val agent = game.findPermanent("Wingshield Agent")!!
                game.state.getEntity(agent)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) shouldBe 1
            }

            test("attacking grants flying to another creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wingshield Agent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wingshield Agent" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gains flying") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
