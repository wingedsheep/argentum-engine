package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.OngoingInvestigation
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Ongoing Investigation (SOI #77) — {1}{U} Enchantment.
 *
 *   Whenever one or more creatures you control deal combat damage to a player, investigate.
 *   {1}{G}, Exile a creature card from your graveyard: Investigate. You gain 2 life.
 */
class OngoingInvestigationScenarioTest : ScenarioTestBase() {

    private val abilityId = OngoingInvestigation.activatedAbilities.first().id

    init {
        test("two creatures connecting investigate once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ongoing Investigation")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.state.stack.size shouldBe 1
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20 - 5
            game.findAllPermanents("Clue") shouldHaveSize 1
        }

        test("exiling a creature card from the graveyard investigates and gains 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ongoing Investigation")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val enchantment = game.findPermanent("Ongoing Investigation")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = enchantment,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.findAllPermanents("Clue") shouldHaveSize 1
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
