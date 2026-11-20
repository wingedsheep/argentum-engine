package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mutant's Prey (DGM #45) — {G} Instant.
 *
 *   Target creature you control with a +1/+1 counter on it fights target creature an opponent
 *   controls.
 */
class MutantsPreyScenarioTest : ScenarioTestBase() {

    private fun buildGame(withCounter: Boolean): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Mutant's Prey")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (withCounter) {
            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { c ->
                c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        }
        return game
    }

    private fun cast(game: TestGame) = game.execute(
        CastSpell(
            game.player1Id,
            game.findCardsInHand(1, "Mutant's Prey").single(),
            listOf(
                ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!),
                ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!),
            ),
        )
    )

    init {
        test("your creature with a +1/+1 counter fights the opponent's creature") {
            val game = buildGame(withCounter = true)

            cast(game).error shouldBe null
            game.resolveStack()

            withClue("the 4/4 Giant kills the Bears and survives their 2 damage") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("a creature without a +1/+1 counter can't be the first target") {
            val game = buildGame(withCounter = false)

            withClue("the first target must have a +1/+1 counter") {
                cast(game).error.shouldNotBeNull()
            }
        }
    }
}
