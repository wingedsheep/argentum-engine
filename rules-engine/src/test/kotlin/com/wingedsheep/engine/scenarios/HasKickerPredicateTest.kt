package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKicker] — "a card with a kicker
 * ability". Kicker and multikicker count (CR 702.33); Offspring, Bargain and a flash-timing unlock
 * ride the same optional-cost rail but are not kicker. The flag is stamped from the printed keyword
 * abilities, so it answers in every zone.
 */
class HasKickerPredicateTest : ScenarioTestBase() {

    private val kicker = card("Test Kicker Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.kicker("{1}{G}"))
    }

    private val multikicker = card("Test Multikicker Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.multikicker("{1}"))
    }

    private val offspring = card("Test Offspring Mouse") {
        manaCost = "{W}"
        typeLine = "Creature — Mouse"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.offspring("{2}"))
    }

    private val flashUnlock = card("Test Flash Unlock Shock") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.flashKicker("{2}"))
    }

    private val bargainer = card("Test Bargain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        bargain()
    }

    private fun TestGame.hasKicker(entityId: EntityId, controllerId: EntityId): Boolean =
        services.predicateEvaluator.matches(
            state,
            state.projectedState,
            entityId,
            GameObjectFilter.Any.withKicker(),
            PredicateContext(controllerId = controllerId)
        )

    init {
        cardRegistry.register(kicker)
        cardRegistry.register(multikicker)
        cardRegistry.register(offspring)
        cardRegistry.register(flashUnlock)
        cardRegistry.register(bargainer)

        test("matches kicker and multikicker cards in every zone, never another optional-cost mechanic") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Test Kicker Bear")
                .withCardInHand(1, "Test Multikicker Bear")
                .withCardInLibrary(1, "Test Kicker Bear")
                .withCardOnBattlefield(1, "Test Kicker Bear")
                .withCardInGraveyard(1, "Test Offspring Mouse")
                .withCardInGraveyard(1, "Test Flash Unlock Shock")
                .withCardInGraveyard(1, "Test Bargain Bear")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val you = game.player1Id

            withClue("kicker, graveyard") {
                game.hasKicker(game.findCardsInGraveyard(1, "Test Kicker Bear").single(), you) shouldBe true
            }
            withClue("multikicker, hand") {
                game.hasKicker(game.findCardsInHand(1, "Test Multikicker Bear").single(), you) shouldBe true
            }
            withClue("kicker, library") {
                game.hasKicker(game.findCardsInLibrary(1, "Test Kicker Bear").single(), you) shouldBe true
            }
            withClue("kicker, battlefield") {
                game.hasKicker(game.findPermanent("Test Kicker Bear")!!, you) shouldBe true
            }
            for (name in listOf("Test Offspring Mouse", "Test Flash Unlock Shock", "Test Bargain Bear", "Centaur Courser")) {
                withClue("not kicker: $name") {
                    game.hasKicker(game.findCardsInGraveyard(1, name).single(), you) shouldBe false
                }
            }
        }
    }
}
