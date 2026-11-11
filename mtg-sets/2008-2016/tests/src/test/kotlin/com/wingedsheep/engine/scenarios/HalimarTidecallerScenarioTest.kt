package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Halimar Tidecaller — its ETB may return a card with awaken (and only such a card) from your
 * graveyard, and it grants flying to land creatures you control, including a freshly awakened land.
 */
class HalimarTidecallerScenarioTest : ScenarioTestBase() {

    private fun etbBoard() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Halimar Tidecaller")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardInGraveyard(1, "Coastal Discovery")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("ETB returns a card with awaken, and a card without awaken is not a legal target") {
            val game = etbBoard()
            game.castSpell(1, "Halimar Tidecaller").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val discovery = game.findCardsInGraveyard(1, "Coastal Discovery").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            withClue("only the awaken card is targetable") {
                decision.legalTargets[0].orEmpty() shouldContain discovery
                decision.legalTargets[0].orEmpty() shouldNotContain bears
            }

            game.selectTargets(listOf(discovery)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Coastal Discovery") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("declining the may leaves the awaken card in the graveyard") {
            val game = etbBoard()
            game.castSpell(1, "Halimar Tidecaller").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val discovery = game.findCardsInGraveyard(1, "Coastal Discovery").single()
            game.selectTargets(listOf(discovery)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Coastal Discovery") shouldBe true
            game.isInHand(1, "Coastal Discovery") shouldBe false
        }

        test("an awakened land you control has flying; a non-creature land and an opponent's land creature don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Halimar Tidecaller")
                .withCardInHand(1, "Coastal Discovery")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardOnBattlefield(2, "Dryad Arbor")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val islands = game.findPermanents("Island")
            val awakened = islands.first()
            val plainLand = islands.last()
            val opposingArbor = game.findPermanents("Dryad Arbor").single()

            game.state.projectedState.hasKeyword(awakened, Keyword.FLYING) shouldBe false

            game.castSpellWithAwaken(1, "Coastal Discovery", listOf(awakened)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(awakened) shouldBe true
            withClue("the awakened land is a land creature you control") {
                projected.hasKeyword(awakened, Keyword.FLYING) shouldBe true
            }
            withClue("a land that isn't a creature gets nothing") {
                projected.hasKeyword(plainLand, Keyword.FLYING) shouldBe false
            }
            withClue("the opponent's land creature isn't yours") {
                projected.isCreature(opposingArbor) shouldBe true
                projected.hasKeyword(opposingArbor, Keyword.FLYING) shouldBe false
            }
            withClue("the Tidecaller itself isn't a land") {
                projected.hasKeyword(game.findPermanent("Halimar Tidecaller")!!, Keyword.FLYING) shouldBe false
            }
        }
    }
}
