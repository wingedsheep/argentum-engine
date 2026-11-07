package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.exo.cards.PeaceOfMind
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Peace of Mind (EXO #13) — {1}{W} Enchantment.
 *
 *   {W}, Discard a card: You gain 3 life.
 *
 * Paying {W} and discarding a card gains 3 life; the discarded card goes to the graveyard.
 */
class PeaceOfMindScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = PeaceOfMind.activatedAbilities.first().id

        test("{W}, discard a card: you gain 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Peace of Mind")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val peace = game.findPermanent("Peace of Mind")!!
            val fodder = game.findCardsInHand(1, "Grizzly Bears").single()
            val lifeBefore = game.getLifeTotal(1)

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = peace,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe lifeBefore + 3
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 0
        }
    }
}
