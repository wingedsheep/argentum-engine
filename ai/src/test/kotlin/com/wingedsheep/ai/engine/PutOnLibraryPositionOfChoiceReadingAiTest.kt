package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target permanent's owner puts it on their choice of the top or bottom of their library"
 * ([com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect]): Dire Downdraft,
 * Aether Gust, Plan for All Outcomes, Deem Inferior's second-from-the-top.
 *
 * The owner picks the position, so at best they draw the card again instead of a fresh one — a
 * worse bounce, read like the plain tuck onto a library: removal aimed at their side. Unread, the
 * card carried no intent (a do-nothing spell to every consumer that prices removal) and its slot no
 * polarity.
 */
class PutOnLibraryPositionOfChoiceReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.onBattlefield(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a tuck the owner positions is removal aimed at their side") {
            for (name in listOf("Dire Downdraft", "Aether Gust", "Deem Inferior")) {
                withClue("$name: its slot reads harmful") {
                    intents.spellTargetPolarities(name) shouldBe listOf(TargetPolarity.HARMFUL)
                }
                withClue("$name: the card is removal") {
                    intents.forName(name)!!.tags shouldContain IntentTag.REMOVAL
                }
            }
            withClue("Plan for All Outcomes: its enter trigger's slot reads harmful") {
                val trigger = cardRegistry.getCard("Plan for All Outcomes")!!.script.triggeredAbilities.first()
                TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Plan for All Outcomes tucks the opponent's creature, not our own") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(1, "Plan for All Outcomes")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Craw Wurm")
                .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Plan for All Outcomes").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            withClue("the tuck reads as removal: Craw Wurm leaves their board, our Elves stay") {
                response.selectedTargets.values.flatten() shouldBe listOf(game.onBattlefield(game.player2Id, "Craw Wurm"))
            }
        }

    }
}
