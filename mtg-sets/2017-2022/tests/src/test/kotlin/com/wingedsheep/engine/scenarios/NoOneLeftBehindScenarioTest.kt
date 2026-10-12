package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * No One Left Behind (BRO) — "This spell costs {3} less to cast if it targets a creature card with
 * mana value 3 or less. Return target creature card from your graveyard to the battlefield."
 *
 * The discounted target lives in the graveyard, so the cast enumerator must find it there: with
 * only {1}{B} available the spell is offered (and castable) exactly when a cheap creature card is
 * in the caster's graveyard.
 */
class NoOneLeftBehindScenarioTest : ScenarioTestBase() {

    private fun castOffers(game: TestGame) = game.getLegalActions(1)
        .filter { (it.action as? CastSpell)?.let { cast -> game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name } == "No One Left Behind" }

    init {
        test("with {1}{B} it is castable on a mana value 3 or less creature card and returns it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "No One Left Behind")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("the discounted cast should be offered as affordable") {
                castOffers(game).any { it.isAffordable } shouldBe true
            }

            val result = game.castSpellTargetingGraveyardCard(1, "No One Left Behind", 1, "Grizzly Bears")
            withClue("cast should succeed for {1}{B}: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "No One Left Behind") shouldBe true
        }

        test("with {1}{B} and only a big creature card in the graveyard it is not affordable") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "No One Left Behind")
                .withCardInGraveyard(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castOffers(game).none { it.isAffordable } shouldBe true
            val result = game.castSpellTargetingGraveyardCard(1, "No One Left Behind", 1, "Craw Wurm")
            result.error shouldNotBe null
        }

        test("a cheap creature card in an opponent's graveyard doesn't make the discount available") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "No One Left Behind")
                .withCardInGraveyard(1, "Craw Wurm")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            castOffers(game).none { it.isAffordable } shouldBe true
        }

        test("at full price it returns a big creature card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "No One Left Behind")
                .withCardInGraveyard(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellTargetingGraveyardCard(1, "No One Left Behind", 1, "Craw Wurm")
            withClue("cast should succeed at full price: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
