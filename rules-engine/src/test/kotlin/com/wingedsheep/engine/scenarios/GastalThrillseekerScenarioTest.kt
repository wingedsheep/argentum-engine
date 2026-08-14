package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gastal Thrillseeker — "Start your engines!", an ETB that drains an opponent for 1, and
 * "Max speed — This creature has deathtouch and haste."
 *
 * The ETB is the card's own speed enabler, so the first case checks the whole chain in one go:
 * entering starts speed at 1 (CR 704.5z state-based action), and the opponent losing life *during
 * your turn* then ticks the inherent speed trigger to 2. The second case pins the max-speed gate —
 * the two keywords must be absent below speed 4 and present at it.
 */
class GastalThrillseekerScenarioTest : ScenarioTestBase() {

    init {
        test("its ETB drains for 1, gains 1, and advances your own speed past the start") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gastal Thrillseeker")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInHand(1, "Gastal Thrillseeker").single()
            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = card,
                    targets = listOf(ChosenTarget.Player(game.player2Id))
                )
            )
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("1 damage to the targeted opponent, 1 life to you") {
                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 21
            }
            withClue(
                "Start your engines! set speed to 1, then the opponent losing life on your turn " +
                    "ticked the inherent trigger to 2"
            ) {
                game.state.speed(game.player1Id) shouldBe 2
            }
            withClue("Each player's speed is tracked separately — the opponent has none") {
                game.state.speed(game.player2Id) shouldBe Speed.NONE
            }
        }

        test("deathtouch and haste only apply at max speed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gastal Thrillseeker", summoningSickness = false)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lizard = game.findPermanent("Gastal Thrillseeker")!!

            withClue("Both keywords sit behind the max-speed gate") {
                game.state.projectedState.hasKeyword(lizard, Keyword.DEATHTOUCH) shouldBe false
                game.state.projectedState.hasKeyword(lizard, Keyword.HASTE) shouldBe false
            }

            game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

            withClue("At speed 4 the gated statics apply") {
                game.state.projectedState.hasKeyword(lizard, Keyword.DEATHTOUCH) shouldBe true
                game.state.projectedState.hasKeyword(lizard, Keyword.HASTE) shouldBe true
            }
        }
    }

    /** Both libraries stocked so nobody decks out on the forced draws. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(8) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}
