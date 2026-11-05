package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A graveyard card an effect puts onto the battlefield **under our control** is ours once it
 * resolves, whoever owns it: "put target creature card from a graveyard onto the battlefield under
 * your control" (Rise from the Grave, Virtue of Persistence), Boneyard Parley's piles. The slot reads
 * [com.wingedsheep.ai.engine.knowledge.TargetPolarity.BENEFICIAL] — good for the card's owner — so
 * the side checks kept every opponent's graveyard card on the wrong side: an optional slot dropped
 * them, a mandatory one fell back to ours, and a spell whose only targets were theirs was never cast.
 */
class TakenGraveyardTargetAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun taken(name: String) = TargetPolarityAnalyzer.takenSlotsForSpell(cardRegistry.getCard(name)!!)

    private fun TestGame.castAction(name: String) = LegalActionEnumerator(
        services.cardRegistry, services.manaSolver, services.costCalculator,
        services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
    ).enumerate(state, player1Id).first {
        (it.action as? CastSpell)?.let { c -> state.getEntity(c.cardId)?.get<CardComponent>()?.name } == name
    }

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(id)?.get<CardComponent>()?.name

    init {
        test("a reanimation under our control out of any graveyard takes its slot; an owner's return does not") {
            taken("Boneyard Parley") shouldBe setOf(0)
            taken("Rise from the Grave") shouldBe setOf(0)
            withClue("to its owner's hand, the card stays its owner's") { taken("March of the Drowned").shouldBeEmpty() }
            withClue("without \"under your control\" the card returns under its owner's") { taken("Zombify").shouldBeEmpty() }
        }

        test("Boneyard Parley's pile reanimation is priced as the recursion it is in the draft heuristic") {
            val terms = LimitedCardRater.explain(cardRegistry.getCard("Boneyard Parley")!!).heuristic
            withClue("$terms") { terms.single { it.part == "Spell" }.value shouldBeGreaterThanOrEqual 0.55 }
        }

        test("Boneyard Parley names the opponent's best creature card, not only ours") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withCardInHand(1, "Boneyard Parley")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val filled = TargetSelection.fillHeuristically(
                game.state, game.castAction("Boneyard Parley"), game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val named = filled.targets.filterIsInstance<ChosenTarget.Card>().map { game.nameOf(it.cardId) }
            withClue("the opponent's Serra Angel comes onto our battlefield as surely as our Bears: $named") {
                named shouldContain "Serra Angel"
            }
        }

        test("Rise from the Grave with only the opponent's creature card to take is still worth casting") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInHand(1, "Rise from the Grave")
                .withCardInGraveyard(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            TargetSelection.aimsOnlyAtTheWrongSide(
                game.state, game.castAction("Rise from the Grave"), game.player1Id, intents,
            ) shouldBe false
        }

        test("Virtue of Persistence's upkeep trigger takes the opponent's best creature card over our lesser one") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(7)
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .withCardOnBattlefield(1, "Virtue of Persistence")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Serra Angel")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .build()
            var passes = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && passes++ < 20) game.passPriority()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().single()
            game.nameOf(target) shouldBe "Serra Angel"
        }
    }
}
