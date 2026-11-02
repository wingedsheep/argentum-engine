package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * All Good Things... (TRK #226).
 *
 *   I, II, III — Draw two cards, then exile a card from your hand.
 *   IV — If all cards exiled with this Saga share a card type, you gain 10 life.
 *
 * Each chapter's exiled card joins the Saga's linked-exile pile; chapter IV gains 10 life only when
 * one card type is common to every card in that pile.
 */
class AllGoodThingsScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "All Good Things...")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }

        fun TestGame.saga(): EntityId = findPermanent("All Good Things...")!!

        /** Sets the Saga to [lore] counters, earlier chapters done and [linked] already exiled with it. */
        fun TestGame.advanceSagaFrom(lore: Int, linked: List<EntityId> = emptyList()) {
            state = state.updateEntity(saga()) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
                    .with(LinkedExileComponent(linked))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        fun TestGame.exiled(name: String): List<EntityId> = state.getExile(player1Id).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        test("chapter II draws two, exiles the chosen card and adds it to the Saga's pile") {
            val game = builder()
                .withCardInHand(1, "Lightning Bolt")
                .withCardInExile(1, "Grizzly Bears")
                .build()
            val earlier = game.exiled("Grizzly Bears").single()
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            val handBefore = game.handSize(1)

            game.advanceSagaFrom(1, linked = listOf(earlier))
            game.resolveStack() // chapter II resolves up to the exile choice
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Lightning Bolt") shouldBe true
            withClue("drew two, then exiled one (the starting player skips the turn-one draw)") {
                game.handSize(1) shouldBe handBefore + 2 - 1
            }
            game.state.getEntity(game.saga())!!.get<LinkedExileComponent>()!!.exiledIds shouldContainExactlyInAnyOrder
                listOf(earlier, bolt)
        }

        test("chapter IV gains 10 life when every exiled card shares a card type") {
            val game = builder()
                .withCardInExile(1, "Grizzly Bears")
                .withCardInExile(1, "Hill Giant")
                .withCardInExile(1, "Ornithopter")
                .build()
            val pile = game.exiled("Grizzly Bears") + game.exiled("Hill Giant") + game.exiled("Ornithopter")

            game.advanceSagaFrom(3, linked = pile)
            game.resolveStack()

            withClue("creature, creature and artifact creature all share creature") {
                game.getLifeTotal(1) shouldBe 30
            }
        }

        test("chapter IV gains nothing when the exiled cards don't all share a card type") {
            val game = builder()
                .withCardInExile(1, "Grizzly Bears")
                .withCardInExile(1, "Hill Giant")
                .withCardInExile(1, "Lightning Bolt")
                .build()
            val pile = game.exiled("Grizzly Bears") + game.exiled("Hill Giant") + game.exiled("Lightning Bolt")

            game.advanceSagaFrom(3, linked = pile)
            game.resolveStack()

            withClue("two creatures share a type, but the instant shares none with them") {
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
