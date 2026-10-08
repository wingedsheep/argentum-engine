package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skystrike Officer (BRO #62) — attacking creates a 1/1 Soldier artifact token; tapping three
 * untapped Soldiers you control draws a card.
 */
class SkystrikeOfficerScenarioTest : ScenarioTestBase() {

    private val drawAbilityId =
        cardRegistry.getCard("Skystrike Officer")!!.activatedAbilities.first().id

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Skystrike Officer", summoningSickness = false)
        .withCardOnBattlefield(1, "Air Marshal")
        .withCardOnBattlefield(1, "Air Marshal")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("attacking creates a 1/1 Soldier artifact creature token") {
            val game = game()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Skystrike Officer" to 2)).error shouldBe null
            game.resolveStack()

            val tokens = game.findAllPermanents("Soldier Token")
            tokens shouldHaveSize 1
            game.state.projectedState.hasType(tokens.single(), "ARTIFACT") shouldBe true
        }

        test("tapping three untapped Soldiers you control draws a card") {
            val game = game()
            val officer = game.findPermanent("Skystrike Officer")!!
            val soldiers = listOf(officer) + game.findAllPermanents("Air Marshal")
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = officer,
                    abilityId = drawAbilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = soldiers)
                )
            ).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("a non-Soldier can't help pay the tap cost") {
            val game = game()
            val officer = game.findPermanent("Skystrike Officer")!!
            val tapped = listOf(officer, game.findAllPermanents("Air Marshal").first(), game.findPermanent("Grizzly Bears")!!)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = officer,
                    abilityId = drawAbilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = tapped)
                )
            ).error shouldNotBe null
        }
    }
}
