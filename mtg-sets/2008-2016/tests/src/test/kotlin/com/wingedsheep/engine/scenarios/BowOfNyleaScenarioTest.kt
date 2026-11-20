package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.BowOfNylea
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bow of Nylea's "{1}{G}, {T}: Choose one —" ability. The graveyard mode is the first pairing of
 * an up-to-four graveyard target slot with a modal activated ability: the mode's chosen targets
 * are gathered and put on the bottom of the library in the order its controller chooses.
 */
class BowOfNyleaScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.activateMode(modePrefix: String) {
            val bow = findPermanent("Bow of Nylea")!!
            execute(ActivateAbility(player1Id, bow, BowOfNylea.activatedAbilities.single().id)).error shouldBe null
            val modes = getPendingDecision() as ChooseOptionDecision
            val index = modes.options.indexOfFirst { it.startsWith(modePrefix) }
            submitDecision(OptionChosenResponse(modes.id, index)).error shouldBe null
        }

        test("the graveyard mode puts only the targeted cards on the bottom of the library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bow of Nylea")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Wind Drake")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val drake = game.findCardsInGraveyard(1, "Wind Drake").single()
            val elves = game.findCardsInGraveyard(1, "Llanowar Elves").single()

            game.activateMode("Put up to four cards")
            game.getPendingDecision() as ChooseTargetsDecision
            game.selectTargets(listOf(bears, drake)).error shouldBe null
            game.resolveStack()

            val order = game.getPendingDecision() as ReorderLibraryDecision
            order.cards.toSet() shouldBe setOf(bears, drake)
            game.submitDecision(OrderedResponse(order.id, listOf(drake, bears))).error shouldBe null

            val library = game.state.getLibrary(game.player1Id)
            library.size shouldBe 3
            library.takeLast(2).toSet() shouldBe setOf(bears, drake)
            game.state.getGraveyard(game.player1Id) shouldBe listOf(elves)
        }

        test("the damage mode can target only a creature with flying and deals 2 damage to it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bow of Nylea")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drake = game.findPermanent("Wind Drake")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activateMode("Deal 2 damage")
            val targeting = game.getPendingDecision() as ChooseTargetsDecision
            targeting.legalTargets[0]!!.contains(drake) shouldBe true
            targeting.legalTargets[0]!!.contains(bears) shouldBe false
            game.selectTargets(listOf(drake)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Wind Drake") shouldBe null
            game.isInGraveyard(2, "Wind Drake") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe bears
        }
    }
}
