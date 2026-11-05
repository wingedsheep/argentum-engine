package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hostage Taker (XLN #223).
 *
 * The exile lasts until Hostage Taker leaves the battlefield, and while the card stays exiled its
 * controller may cast it spending any type of mana. A card already cast is a new object: it stays
 * with its caster when Hostage Taker later leaves. An uncast card returns to its owner.
 */
class HostageTakerScenarioTest : ScenarioTestBase() {

    init {
        context("Hostage Taker") {

            test("the hostage is castable with any type of mana and stays with its caster after Hostage Taker dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hostage Taker")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 10)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hostage Taker").error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the ETB exiled the opponent's creature into its owner's exile") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInExile(2, "Hill Giant") shouldBe true
                }

                val exiledGiant = game.state.getExile(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }

                withClue("Hostage Taker's controller may cast it for as long as it stays exiled, with any mana type") {
                    val permission = game.state.mayPlayPermissions.first { exiledGiant in it.cardIds }
                    permission.controllerId shouldBe game.player1Id
                    permission.permanent shouldBe true
                    permission.withAnyManaType shouldBe true
                }

                // Only Swamps remain untapped: the {3}{R} Giant is paid entirely with black mana.
                val result = game.execute(
                    CastSpell(game.player1Id, exiledGiant, paymentStrategy = PaymentStrategy.AutoPay)
                )
                withClue("the exiled red creature is castable spending Swamp mana") {
                    result.error shouldBe null
                }
                var guard = 0
                while ((game.getPendingDecision() != null || game.state.stack.isNotEmpty()) && guard++ < 40) {
                    when (game.getPendingDecision()) {
                        is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                        null -> game.resolveStack()
                        else -> error("Unexpected decision casting from exile: ${game.getPendingDecision()}")
                    }
                }

                val stolenGiant = game.findPermanent("Hill Giant")
                stolenGiant.shouldNotBeNull()
                game.state.projectedState.getController(stolenGiant) shouldBe game.player1Id

                val taker = game.findPermanent("Hostage Taker")!!
                game.castSpell(1, "Murder", taker).error shouldBe null
                game.resolveStack()

                withClue("Hostage Taker died, but the cast Giant is a new object and stays with its caster") {
                    game.isOnBattlefield("Hostage Taker") shouldBe false
                    val giantNow = game.findPermanent("Hill Giant")
                    giantNow.shouldNotBeNull()
                    game.state.projectedState.getController(giantNow) shouldBe game.player1Id
                    game.isInExile(2, "Hill Giant") shouldBe false
                }
            }

            test("an uncast hostage returns to its owner when Hostage Taker leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hostage Taker")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hostage Taker").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
                game.resolveStack()
                game.isInExile(2, "Hill Giant") shouldBe true

                val taker = game.findPermanent("Hostage Taker")!!
                game.castSpell(1, "Murder", taker).error shouldBe null
                game.resolveStack()

                withClue("the still-exiled Giant returns to the battlefield under its owner's control") {
                    game.isInExile(2, "Hill Giant") shouldBe false
                    val giant = game.findPermanent("Hill Giant")
                    giant.shouldNotBeNull()
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                }
            }
        }
    }
}
