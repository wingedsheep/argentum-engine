package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Óin the Brave — "As long as you have an enduring story, Óin gets +1/+0 and has haste" plus an
 * ungated "{1}, {T}, Discard a card: Draw a card."
 *
 * The haste half is the clearest case for storied being a continuously-checked state-based action
 * rather than an enters-the-battlefield trigger: Óin is a two-drop, so the turn he lands is the turn
 * you are least likely to control three artifacts/legendaries/Sagas. A trigger would sample the count
 * then and Óin would never gain haste on any later turn.
 *
 * The looter is deliberately outside the gate — it works with or without the designation — so it gets
 * its own test on a board that has no enduring story at all. The mechanic's own rules live in
 * [StoriedEnduringStoryTest].
 */
class OinTheBraveScenarioTest : ScenarioTestBase() {

    private val looterAbilityId by lazy {
        cardRegistry.requireCard("Óin the Brave").activatedAbilities[0].id
    }

    init {
        test("without an enduring story Óin is a plain 1/3 with no haste") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Óin the Brave")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oin = game.findPermanent("Óin the Brave")!!
            // Óin is legendary, so he is one of the three himself; the two Mountains are not
            // artifacts, Sagas, or legendary and do not count.
            EnduringStoryService.has(game.state, game.player1Id) shouldBe false
            game.state.projectedState.getPower(oin) shouldBe 1
            game.state.projectedState.getToughness(oin) shouldBe 3
            game.state.projectedState.hasKeyword(oin, Keyword.HASTE) shouldBe false
        }

        test("with an enduring story Óin is a 2/3 with haste") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardOnBattlefield(1, "Ori, Keeper of Songs")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oin = game.findPermanent("Óin the Brave")!!
            EnduringStoryService.has(game.state, game.player1Id) shouldBe true
            game.state.projectedState.getPower(oin) shouldBe 2
            game.state.projectedState.getToughness(oin) shouldBe 3
            game.state.projectedState.hasKeyword(oin, Keyword.HASTE) shouldBe true
        }

        test("the looter draws and discards, and needs no enduring story") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Óin the Brave")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Shock")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oin = game.findPermanent("Óin the Brave")!!
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            EnduringStoryService.has(game.state, game.player1Id) shouldBe false

            // Discard is part of the cost, so it is paid on activation, before the draw resolves:
            // Lightning Bolt leaves the hand now and Shock arrives when the ability resolves.
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = oin,
                    abilityId = looterAbilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(bolt))
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true

            game.resolveStack()
            game.isInHand(1, "Shock") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("the looter can't be activated without the {1}") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardInHand(1, "Lightning Bolt")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oin = game.findPermanent("Óin the Brave")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = oin, abilityId = looterAbilityId)
            ).error shouldNotBe null
        }
    }
}
