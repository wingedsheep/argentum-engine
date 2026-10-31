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
 * Momentum Rumbler (IKO #126) — {3}{R} 3/3 Creature — Dinosaur.
 *
 * "Whenever this creature attacks, if it doesn't have first strike, put a first strike counter on it.
 *  Whenever this creature attacks, if it has first strike, it gains double strike until end of turn."
 *
 * Both triggers are intervening-if on the source's projected keywords: on the first attack only the
 * counter trigger fires, and once the counter grants first strike only the double-strike one does.
 */
class MomentumRumblerScenarioTest : ScenarioTestBase() {

    init {
        context("Momentum Rumbler") {
            test("first attack without first strike puts a first strike counter on it, no double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Momentum Rumbler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val rumbler = game.findPermanent("Momentum Rumbler")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Momentum Rumbler" to 2)).error shouldBe null
                game.resolveStack()

                withClue("gets a first strike counter") {
                    game.state.getEntity(rumbler)?.get<CountersComponent>()?.getCount(CounterType.FIRST_STRIKE) shouldBe 1
                }
                withClue("now has first strike from the counter") {
                    game.state.projectedState.hasKeyword(rumbler, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("the double strike trigger did not fire — it lacked first strike as it attacked") {
                    game.state.projectedState.hasKeyword(rumbler, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }

            test("attacking with first strike grants double strike and no second counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Momentum Rumbler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val rumbler = game.findPermanent("Momentum Rumbler")!!
                game.state = game.state.updateEntity(rumbler) {
                    it.with(CountersComponent().withAdded(CounterType.FIRST_STRIKE, 1))
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Momentum Rumbler" to 2)).error shouldBe null
                game.resolveStack()

                withClue("gains double strike") {
                    game.state.projectedState.hasKeyword(rumbler, Keyword.DOUBLE_STRIKE) shouldBe true
                }
                withClue("no second first strike counter") {
                    game.state.getEntity(rumbler)?.get<CountersComponent>()?.getCount(CounterType.FIRST_STRIKE) shouldBe 1
                }
            }
        }
    }
}
