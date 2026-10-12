package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Meticulous Excavation (BRO #16).
 *
 * "{2}{W}: Return target permanent you control to its owner's hand. If it has unearth, instead
 *  exile it, then return that card to its owner's hand. Activate only during your turn."
 *
 * The interesting case is an unearthed permanent: its "if it would leave the battlefield, exile it
 * instead" replacement would turn a plain bounce into exile, so the card exiles it itself and then
 * returns the card from exile to hand (the 2022-10-14 ruling).
 */
class MeticulousExcavationScenarioTest : ScenarioTestBase() {

    private val excavationAbilityId =
        cardRegistry.getCard("Meticulous Excavation")!!.activatedAbilities.first().id

    private val unearthAbilityId =
        cardRegistry.getCard("Yotian Frontliner")!!.activatedAbilities.first().id

    private fun TestGame.excavate(target: String) {
        val source = findPermanent("Meticulous Excavation")!!
        val result = execute(
            ActivateAbility(
                player1Id, source, excavationAbilityId,
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
            )
        )
        withClue("Activating Meticulous Excavation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Meticulous Excavation") {

            test("returns a permanent without unearth to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meticulous Excavation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.excavate("Grizzly Bears")

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("an unearthed permanent is exiled, then returned to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meticulous Excavation")
                    .withCardInGraveyard(1, "Yotian Frontliner")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val inGraveyard = game.findCardsInGraveyard(1, "Yotian Frontliner").first()
                game.execute(
                    ActivateAbility(game.player1Id, inGraveyard, unearthAbilityId)
                ).error shouldBe null
                game.resolveStack()
                game.findPermanent("Yotian Frontliner") shouldNotBe null

                game.excavate("Yotian Frontliner")

                withClue("The exile-instead replacement doesn't strand it in exile") {
                    game.isInExile(1, "Yotian Frontliner") shouldBe false
                    game.isInHand(1, "Yotian Frontliner") shouldBe true
                }
            }

            test("can't be activated during an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meticulous Excavation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, game.findPermanent("Meticulous Excavation")!!, excavationAbilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    )
                )
                (result.error != null) shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
