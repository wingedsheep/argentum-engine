package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
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
 * Two target readings that were unread, both the pipeline or combined spelling of a single-effect
 * reading the AI already has:
 *
 * - **A graveyard return spelled as gather + move.** "Return target creature card from your
 *   graveyard to your hand" as `moveTracked(gather(CardSource.Entity(t)), ToZone(HAND))` (Foul
 *   Renewal, which reads the toughness of the card that came back), and "put any number of target
 *   creature cards from your graveyard on top of your library" as `toLibraryTop(gather(ChosenTargets))`
 *   (Gravepurge, Forever Young): the single-card recursion [TargetPolarityAnalyzer] reads off a
 *   `MoveToZoneEffect`. The slot is a gift, so the best card is the one to name. Unread, it kept the
 *   removal ranking, which orders our own cards worst first.
 * - **A drain.** "Target opponent loses N life … you gain that much life" (Foul-Tongue Shriek), "target
 *   player loses 2 life and you gain 2 life" (Bloodhunter Bat) as one `DrainLifeEffect`: the
 *   LoseLife half hurts the player it names, as the single LoseLife does.
 */
class PipelineRecursionAndDrainLifePolarityAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a pipeline returning a graveyard target reads as recursion") {
            withClue("Foul Renewal: the graveyard card is ours to get back") {
                TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Foul Renewal")!!)[0] shouldBe TargetPolarity.BENEFICIAL
            }
            for (name in listOf("Gravepurge", "Forever Young")) {
                withClue("$name: the cards go back on top of our library to draw") {
                    TargetPolarityAnalyzer.forSpell(cardRegistry.getCard(name)!!) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
            }
            for (name in listOf("Foul Renewal", "Gravepurge", "Forever Young")) {
                withClue(name) { AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true }
            }
        }

        test("a drain hurts the player it targets") {
            withClue("Foul-Tongue Shriek") {
                TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Foul-Tongue Shriek")!!) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            for (name in listOf("Foul-Tongue Shriek", "Bloodhunter Bat")) {
                withClue(name) { AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true }
            }
        }

        test("the heuristic fill returns Foul Renewal's best creature card, not its cheapest") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Foul Renewal")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val enumerator = LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
            )
            val cast = enumerator.enumerate(game.state, game.player1Id).first {
                (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name } == "Foul Renewal"
            }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val returned = filled.targets.filterIsInstance<ChosenTarget.Card>().single().cardId
            withClue("recursion reads as a gift, so the biggest card comes back — and shrinks the most") {
                game.state.getEntity(returned)?.get<CardComponent>()?.name shouldBe "Serra Angel"
            }
        }
    }
}
