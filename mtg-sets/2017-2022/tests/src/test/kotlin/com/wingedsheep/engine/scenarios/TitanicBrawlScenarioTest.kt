package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Titanic Brawl (RNA #146) — your creature fights one you don't control; {1} cheaper when your
 * fighter carries a +1/+1 counter.
 */
class TitanicBrawlScenarioTest : ScenarioTestBase() {
    init {
        fun board(forests: Int, counter: Boolean, counterOn: String = "Hill Giant"): TestGame {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Titanic Brawl")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", forests)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            if (counter) {
                val holder = game.findPermanent(counterOn)!!
                game.state = game.state.updateEntity(holder) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
            }
            return game
        }

        fun cast(game: TestGame) = game.execute(
            CastSpell(
                game.player1Id,
                game.findCardsInHand(1, "Titanic Brawl").single(),
                listOf(
                    ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!),
                    ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!),
                ),
            )
        )

        test("the two creatures fight") {
            val game = board(forests = 2, counter = false)
            val r = cast(game)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            withClue("the 3/3 Giant took 2 and survives") { game.isOnBattlefield("Hill Giant") shouldBe true }
        }

        test("a +1/+1 counter on your creature makes it castable for {G}") {
            val game = board(forests = 1, counter = true)
            val r = cast(game)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("without the counter one Forest is not enough") {
            val game = board(forests = 1, counter = false)
            cast(game).error shouldNotBe null
        }

        test("a counter on the opponent's creature gives no discount") {
            val game = board(forests = 1, counter = true, counterOn = "Grizzly Bears")
            cast(game).error shouldNotBe null
        }
    }
}
