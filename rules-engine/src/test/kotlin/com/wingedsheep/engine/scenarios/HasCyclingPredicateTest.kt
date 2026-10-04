package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasCycling] — "a card with a cycling
 * ability". Plain cycling and typecycling both count (CR 702.29e); the flag is stamped from the
 * printed keyword abilities, so it answers in every zone.
 */
class HasCyclingPredicateTest : ScenarioTestBase() {

    private val plainCycler = card("Test Plain Cycler") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Lizard"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.cycling("{2}"))
    }

    private val landcycler = card("Test Swampcycler") {
        manaCost = "{5}{B}"
        typeLine = "Creature — Horror"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.typecycling("Swamp", "{2}"))
    }

    private fun TestGame.hasCycling(entityId: EntityId, controllerId: EntityId): Boolean =
        services.predicateEvaluator.matches(
            state,
            state.projectedState,
            entityId,
            GameObjectFilter.Any.withCycling(),
            PredicateContext(controllerId = controllerId)
        )

    init {
        cardRegistry.register(plainCycler)
        cardRegistry.register(landcycler)

        test("matches cycling and typecycling cards in every zone, never a card without cycling") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Test Plain Cycler")
                .withCardInHand(1, "Test Swampcycler")
                .withCardInLibrary(1, "Test Plain Cycler")
                .withCardOnBattlefield(1, "Test Plain Cycler")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val you = game.player1Id

            withClue("plain cycling, graveyard") {
                game.hasCycling(game.findCardsInGraveyard(1, "Test Plain Cycler").single(), you) shouldBe true
            }
            withClue("typecycling, hand") {
                game.hasCycling(game.findCardsInHand(1, "Test Swampcycler").single(), you) shouldBe true
            }
            withClue("plain cycling, library") {
                game.hasCycling(game.findCardsInLibrary(1, "Test Plain Cycler").single(), you) shouldBe true
            }
            withClue("plain cycling, battlefield") {
                game.hasCycling(game.findPermanent("Test Plain Cycler")!!, you) shouldBe true
            }
            withClue("no cycling") {
                game.hasCycling(game.findCardsInGraveyard(1, "Centaur Courser").single(), you) shouldBe false
            }
        }
    }
}
