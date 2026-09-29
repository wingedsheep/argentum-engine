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
 * Converter Beast (MOM #180) — "When this creature enters, incubate 5."
 */
class ConverterBeastScenarioTest : ScenarioTestBase() {
    init {
        test("entering incubates 5") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Converter Beast")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Converter Beast").error shouldBe null
            game.resolveStack()

            game.findPermanent("Converter Beast") shouldNotBe null
            val incubator = game.findPermanent("Incubator")
            withClue("an Incubator with five +1/+1 counters") {
                incubator shouldNotBe null
                game.state.getEntity(incubator!!)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
            }
        }
    }
}
