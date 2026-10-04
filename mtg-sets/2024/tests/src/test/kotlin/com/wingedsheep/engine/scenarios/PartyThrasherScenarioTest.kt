package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Party Thrasher (MH3 #129) — {1}{R} 1/4. "Noncreature spells you cast from exile have convoke.
 * At the beginning of your first main phase, you may discard a card. If you do, exile the top
 * two cards of your library, then choose one of them. You may play that card this turn."
 */
class PartyThrasherScenarioTest : ScenarioTestBase() {

    private fun nameOf(game: TestGame, id: EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    private fun setup(libraryCard: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Party Thrasher")
        .withCardInHand(1, "Island")
        .withCardInHand(1, "Mountain")
        .withCardInLibrary(1, libraryCard)
        .withCardInLibrary(1, libraryCard)
        .withCardInLibrary(1, libraryCard)
        .withActivePlayer(1)
        .inPhase(Phase.BEGINNING, Step.UPKEEP) // turn 1: the starting player skips the draw
        .build()

    /** Pass into the first main phase, accept the trigger, discard the Island, return the choice. */
    private fun acceptAndChoose(game: TestGame): Pair<EntityId, List<EntityId>> {
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        game.resolveStack()
        game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        game.answerYesNo(true).error shouldBe null

        val discard = game.getPendingDecision()
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        val island = discard.options.first { nameOf(game, it) == "Island" }
        game.selectCards(listOf(island)).error shouldBe null

        val choose = game.getPendingDecision()
        choose.shouldBeInstanceOf<SelectCardsDecision>()
        choose.options shouldHaveSize 2
        val picked = choose.options.first()
        game.selectCards(listOf(picked)).error shouldBe null
        game.resolveStack()
        withClue("the Island was discarded") { game.isInGraveyard(1, "Island") shouldBe true }
        return picked to choose.options
    }

    init {
        context("Party Thrasher") {

            test("discard, exile two, play the chosen noncreature spell paid by convoke") {
                val game = setup("Lightning Bolt")
                val (picked, exiled) = acceptAndChoose(game)
                exiled.forEach { game.state.getExile(game.player1Id).contains(it) shouldBe true }
                val other = exiled.single { it != picked }

                val actions = game.getLegalActions(1)
                val pickedCast = actions.firstOrNull {
                    (it.action as? CastSpell)?.cardId == picked && it.sourceZone == "EXILE"
                }
                withClue("the chosen card is castable from exile with convoke") {
                    pickedCast shouldNotBe null
                    pickedCast!!.hasConvoke shouldBe true
                }
                withClue("the other exiled card is not playable") {
                    actions.none { (it.action as? CastSpell)?.cardId == other } shouldBe true
                }

                // No lands: tapping the red Thrasher pays {R}.
                val thrasher = game.findPermanent("Party Thrasher")!!
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = picked,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(thrasher to ConvokePayment(color = Color.RED))
                        )
                    )
                )
                withClue("convoke pays for the noncreature exile cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("a creature spell cast from exile does not get convoke") {
                val game = setup("Grizzly Bears")
                val (picked, _) = acceptAndChoose(game)
                val cast = game.getLegalActions(1).firstOrNull {
                    (it.action as? CastSpell)?.cardId == picked && it.sourceZone == "EXILE"
                }
                withClue("the creature is offered from exile but without convoke") {
                    cast shouldNotBe null
                    cast!!.hasConvoke shouldBe false
                }
            }

            test("declining the discard exiles nothing") {
                val game = setup("Lightning Bolt")
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()
                game.state.getExile(game.player1Id) shouldHaveSize 0
                game.isInHand(1, "Island") shouldBe true
                game.librarySize(1) shouldBe 3
            }
        }
    }
}
