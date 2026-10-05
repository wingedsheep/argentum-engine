package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Chains of Custody (J22 #3) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature you control
 *   When this Aura enters, exile target nonland permanent an opponent controls until this Aura
 *   leaves the battlefield.
 *   Enchanted creature has ward {2}.
 */
class ChainsOfCustodyScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun exileBoard() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Chains of Custody")
        .withCardInHand(1, "Disenchant")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Bonesplitter")
        .withLandsOnBattlefield(2, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Chains of Custody") {

            test("exiles an opponent's nonland permanent until the Aura leaves the battlefield") {
                val game = exileBoard()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                game.castSpell(1, "Chains of Custody", bears).error shouldBe null
                game.payIfAsked()
                game.resolveStack() // Aura enters attached to the Bears -> ETB trigger asks for a target

                val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
                withClue("only nonland permanents an opponent controls: not the Forest, not your own Bears or Aura") {
                    decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(giant, bonesplitter)
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false

                val chains = game.findPermanent("Chains of Custody")!!
                game.castSpell(1, "Disenchant", chains).error shouldBe null
                game.payIfAsked()
                game.resolveStack() // Disenchant, then the leaves-the-battlefield return

                withClue("the Hill Giant returns under its owner's control once the Aura is gone") {
                    game.isInGraveyard(1, "Chains of Custody") shouldBe true
                    val returned = game.findPermanent("Hill Giant").shouldNotBeNull()
                    game.state.projectedState.getController(returned) shouldBe game.player2Id
                }
            }

            test("if the Aura leaves before its enters trigger resolves, the target is never exiled") {
                val game = exileBoard()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Chains of Custody", bears).error shouldBe null
                game.payIfAsked()
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null

                // The enters trigger is on the stack; destroy the Aura in response.
                val chains = game.findPermanent("Chains of Custody")!!
                game.castSpell(1, "Disenchant", chains).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Chains of Custody") shouldBe true
                withClue("the Hill Giant never left the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isInExile(2, "Hill Giant") shouldBe false
                }
            }

            test("the enchanted creature has ward {2}: an opponent's spell is countered unless they pay") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Chains of Custody", "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.WARD) shouldBe true

                game.castSpell(1, "Shock", bears).error shouldBe null
                game.payIfAsked()
                game.resolveStack() // ward trigger resolves; the caster can't pay {2}, so Shock is countered

                withClue("Shock was countered by ward — the Bears survive") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Shock") shouldBe true
                }
            }
        }
    }
}
