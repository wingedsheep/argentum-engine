package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m19.cards.ChromiumTheMutable
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Chromium, the Mutable (M19 #214) — "Discard a card: Until end of turn, Chromium becomes a Human
 * with base power and toughness 1/1, loses all abilities, and gains hexproof. It can't be blocked
 * this turn."
 *
 * Proves the layer ordering: the creature type is replaced (not added), the card stays a legendary
 * creature, flying is stripped, and hexproof / can't-be-blocked — granted after the strip — survive.
 */
class ChromiumTheMutableScenarioTest : ScenarioTestBase() {

    init {
        test("discard a card: becomes a 1/1 Human with only hexproof, and can't be blocked") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Chromium, the Mutable")
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chromium = game.findPermanent("Chromium, the Mutable")!!
            val shock = game.findCardsInHand(1, "Shock").single()

            game.state.projectedState.hasKeyword(chromium, Keyword.FLYING) shouldBe true

            game.execute(
                ActivateAbility(
                    game.player1Id, chromium, ChromiumTheMutable.activatedAbilities.single().id,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(shock)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Shock") shouldBe true

            val projected = game.state.projectedState
            projected.getPower(chromium) shouldBe 1
            projected.getToughness(chromium) shouldBe 1
            projected.getSubtypes(chromium) shouldBe setOf("Human")
            projected.hasType(chromium, "CREATURE") shouldBe true
            projected.isLegendary(chromium) shouldBe true
            projected.hasKeyword(chromium, Keyword.FLYING) shouldBe false
            projected.hasKeyword(chromium, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(chromium, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }
    }
}
