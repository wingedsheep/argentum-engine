package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unity of Purpose (OGW #66, {3}{U}, Instant).
 *
 *   Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 *   Untap each creature you control with a +1/+1 counter on it.
 *
 * The untap group is read after support has placed its counters, so a tapped creature you control that
 * was just supported untaps; a supported opponent's creature keeps its counter but stays tapped, and an
 * unsupported creature of yours without a counter stays tapped.
 */
class UnityOfPurposeScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.isTapped(id: EntityId) = state.getEntity(id)?.get<TappedComponent>() != null

    private fun ScenarioTestBase.TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("supported creatures you control untap; the supported opposing creature and an unsupported one stay tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Unity of Purpose")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Craw Wurm", tapped = true)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val spell = game.findCardsInHand(1, "Unity of Purpose").first()

            val cast = game.execute(
                CastSpell(
                    game.player1Id, spell,
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(wurm)),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("both supported creatures got one counter") {
                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(wurm) shouldBe 1
                game.plusOneCounters(giant) shouldBe 0
            }
            withClue("Grizzly Bears got its counter from support, so it untaps") { game.isTapped(bears) shouldBe false }
            withClue("Craw Wurm isn't yours, so it stays tapped") { game.isTapped(wurm) shouldBe true }
            withClue("Hill Giant has no counter, so it stays tapped") { game.isTapped(giant) shouldBe true }
        }

        test("cast with zero support targets: a creature without a counter stays tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Unity of Purpose")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.execute(
                CastSpell(game.player1Id, game.findCardsInHand(1, "Unity of Purpose").first(), targets = emptyList())
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("zero support targets: no counter, so Grizzly Bears stays tapped") {
                game.plusOneCounters(bears) shouldBe 0
                game.isTapped(bears) shouldBe true
            }
        }
    }
}
