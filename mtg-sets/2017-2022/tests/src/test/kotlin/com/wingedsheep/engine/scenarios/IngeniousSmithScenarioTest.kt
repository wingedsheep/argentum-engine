package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ingenious Smith (AFR #21) — {1}{W} Creature — Human Artificer 1/1.
 *
 *   When this creature enters, look at the top four cards of your library. You may reveal an
 *   artifact card from among them and put it into your hand. Put the rest on the bottom of your
 *   library in a random order.
 *   Whenever one or more artifacts you control enter, put a +1/+1 counter on this creature. This
 *   ability triggers only once each turn.
 */
class IngeniousSmithScenarioTest : ScenarioTestBase() {

    init {
        test("ETB puts a revealed artifact into hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ingenious Smith")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Ornithopter")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ornithopter = game.findCardsInLibrary(1, "Ornithopter").single()

            game.castSpell(1, "Ingenious Smith").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(ornithopter)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Ornithopter") shouldBe true
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
        }

        test("artifacts entering add only one +1/+1 counter per turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ingenious Smith")
                .withCardsInHand(1, "Ornithopter", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val smith = game.findPermanent("Ingenious Smith")!!
            fun counters() = game.state.getEntity(smith)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            withClue("the first artifact entering adds a counter") { counters() shouldBe 1 }

            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            withClue("the ability triggers only once each turn") { counters() shouldBe 1 }
        }
    }
}
