package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Drizzt Do'Urden (AFR #220) — the intervening-if dies trigger: a creature with greater
 * last-known power grows Drizzt by the difference; one with equal power does nothing.
 */
class DrizztDoUrdenScenarioTest : ScenarioTestBase() {

    init {
        context("Drizzt Do'Urden") {

            test("a dying creature with greater power adds the difference as +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drizzt Do'Urden")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drizzt = game.findPermanent("Drizzt Do'Urden")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Doom Blade", wurm).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Craw Wurm") shouldBe true
                withClue("Craw Wurm's 6 power minus Drizzt's 3") {
                    game.state.getEntity(drizzt)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                    game.state.projectedState.getPower(drizzt) shouldBe 6
                }
            }

            test("a dying creature with equal power does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drizzt Do'Urden")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drizzt = game.findPermanent("Drizzt Do'Urden")!!
                game.castSpell(1, "Doom Blade", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                (game.state.getEntity(drizzt)!!.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                game.state.projectedState.getPower(drizzt) shouldBe 3
            }
        }
    }
}
