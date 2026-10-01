package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thirsting Roots (ONE #185) — {G} Sorcery.
 *
 * Choose one —
 * • Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * • Proliferate.
 */
class ThirstingRootsScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("mode 1 tutors a basic land to hand, ignoring nonbasic cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Thirsting Roots")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findCardsInLibrary(1, "Forest").single()

            game.castSpellWithMode(1, "Thirsting Roots", 0).error shouldBe null
            game.resolveStack()

            withClue("a search decision is pending") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(forest))
            if (!game.hasPendingDecision()) game.resolveStack()

            withClue("Forest moved to hand") { game.isInHand(1, "Forest") shouldBe true }
            withClue("Bears stayed in library") { game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 2 }
            game.isInGraveyard(1, "Thirsting Roots") shouldBe true
        }

        test("mode 2 proliferates the chosen permanent") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Thirsting Roots")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            game.castSpellWithMode(1, "Thirsting Roots", 1).error shouldBe null
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) game.selectCards(listOf(bears)) else game.resolveStack()
            }

            plusOnes(game, bears) shouldBe 2
            game.isInGraveyard(1, "Thirsting Roots") shouldBe true
        }
    }
}
