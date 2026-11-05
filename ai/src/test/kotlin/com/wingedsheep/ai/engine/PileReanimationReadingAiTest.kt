package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Graveyard cards a spell brings back through a pipeline or a mode, read as the recursion they are.
 *
 * - "Exile up to five target creature cards from graveyards. An opponent separates those cards into
 *   two piles. Put all cards from the pile of your choice onto the battlefield under your control"
 *   (Boneyard Parley): the chosen targets, however they are split, end up on our battlefield. Unread,
 *   the slot fell to the removal ranking and named the cheapest creature cards.
 * - A modal spell's mode reads against its own targets, and a for-each over one target slot against
 *   that slot: March of the Drowned's "return target creature card from your graveyard to your hand"
 *   read as a bounce — removal — because its slot was never consulted.
 * - "This creature can't attack unless you control an artifact" (Desperate Castaways) is a drawback on
 *   its own attacks, as needing a co-attacker is.
 */
class PileReanimationReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags

    init {
        test("a pile reanimation over graveyard targets reads as a beneficial recursion") {
            TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Boneyard Parley")!!) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            tags("Boneyard Parley") shouldContain IntentTag.RECURSION
            withClue("Fact or Fiction's piles go to hand from the library: still a draw, not a recursion") {
                tags("Fact or Fiction") shouldContain IntentTag.DRAW
                tags("Fact or Fiction") shouldNotContain IntentTag.RECURSION
            }
        }

        test("a mode returning cards from our graveyard to hand is recursion, not removal") {
            tags("March of the Drowned") shouldContain IntentTag.RECURSION
            tags("March of the Drowned") shouldNotContain IntentTag.REMOVAL
        }

        test("can't attack unless you control an artifact is a combat restriction on the body") {
            tags("Desperate Castaways") shouldContain IntentTag.COMBAT_RESTRICTION
        }

        test("the heuristic fill names the best creature card for Boneyard Parley, not the cheapest") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withCardInHand(1, "Boneyard Parley")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInGraveyard(1, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val enumerator = LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
            )
            val cast = enumerator.enumerate(game.state, game.player1Id).first {
                (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name } == "Boneyard Parley"
            }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val named = filled.targets.filterIsInstance<ChosenTarget.Card>()
                .map { game.state.getEntity(it.cardId)?.get<CardComponent>()?.name }
            withClue("the reanimation is a gift, so the best creature card is named, not the cheapest: $named") {
                named shouldBe listOf("Serra Angel")
            }
        }
    }
}
