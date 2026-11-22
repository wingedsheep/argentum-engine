package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m14.cards.RingOfThreeWishes
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ring of Three Wishes (M14 #216) — enters with three wish counters; "{5}, {T}, Remove a wish
 * counter: Search your library for a card, put that card into your hand, then shuffle."
 */
class RingOfThreeWishesScenarioTest : ScenarioTestBase() {

    init {
        context("Ring of Three Wishes") {
            test("enters with three wish counters and spends one to tutor any card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ring of Three Wishes")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ring of Three Wishes").error shouldBe null
                game.resolveStack()
                val ring = game.findPermanent("Ring of Three Wishes")!!
                fun wishes() = game.state.getEntity(ring)?.get<CountersComponent>()?.getCount(CounterType.WISH) ?: 0
                wishes() shouldBe 3

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ring,
                        abilityId = RingOfThreeWishes.activatedAbilities.first().id,
                    )
                ).error shouldBe null
                withClue("a wish counter is removed as a cost") { wishes() shouldBe 2 }
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.librarySize(1) shouldBe 1
                game.isTapped(ring) shouldBe true
            }
        }
    }

    private fun TestGame.isTapped(id: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(id)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == true
}
