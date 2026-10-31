package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The target polarity of a move *out of a graveyard* into a hand or onto the battlefield
 * ([com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect]) — recursion.
 *
 * Unread, a recursion slot kept the removal ranking, which ranks a card that isn't an opponent's
 * permanent by its *negated* value: Restore with Empathy took back the cheapest card in the
 * graveyard. The move is a gift to whoever gets the card, so the slot reads beneficial, and the
 * best card in the graveyard is the one to return.
 */
class GraveyardRecursionReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a return from the graveyard reads as beneficial, whether or not the move names its zone") {
            for (name in listOf("Restore with Empathy", "Generous Revival")) {
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(cardRegistry.getCard(name)!!) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        // The heuristic fill is the AI's cheap path: the baseline every committed cast starts from,
        // and the whole decision wherever there is no budget to simulate each candidate.
        test("the heuristic fill aims Restore with Empathy at the best permanent card, not the cheapest") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInHand(1, "Restore with Empathy")
                .withCardInGraveyard(1, "Plains")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInGraveyard(1, "Island")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val enumerator = LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
            )
            val cast = enumerator.enumerate(game.state, game.player1Id).first {
                (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name } == "Restore with Empathy"
            }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val returned = filled.targets.filterIsInstance<ChosenTarget.Card>().single().cardId
            withClue("recursion reads as a gift, so the best card comes back") {
                game.state.getEntity(returned)?.get<CardComponent>()?.name shouldBe "Serra Angel"
            }
        }
    }
}
