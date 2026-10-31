package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The intent of a counter put on another permanent
 * ([com.wingedsheep.sdk.scripting.effects.AddCountersEffect]): a +1/+1 counter is a pump that never
 * wears off, a -1/-1 counter on someone else's creature is removal.
 *
 * Unread, a flash creature whose arrival grows a teammate looked like it did nothing on arrival,
 * so [com.wingedsheep.ai.engine.knowledge.AmbushWindow] held it for the opponent's turn — and the
 * counter that would have joined this turn's attack waited a turn with it.
 */
class AddCountersReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a +1/+1 counter on a target is a pump; one on the card itself still needs no tag") {
            withClue("Koth of the Homestead: put a +1/+1 counter on target creature") {
                intents.forName("Koth of the Homestead")!!.tags shouldContain IntentTag.PUMP
            }
            withClue("Little Bear: untap, then a +1/+1 counter on the Bear it untapped") {
                intents.forName("Little Bear")!!.tags shouldContain IntentTag.PUMP
            }
            withClue("Edgar's end-step counters go on Edgar alone; its activation reaches the team") {
                val edgar = cardRegistry.getCard("Edgar, Moonlit Sovereign")!!
                com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
                    .analyzeEffect(edgar.script.triggeredAbilities.first().effect).tags shouldNotContain IntentTag.PUMP
            }
        }

        // instants-12's shape: a flash creature whose arrival changes this turn's attack is worth
        // having before we swing, so the ambush hold steps aside and the board decides.
        test("casts Little Bear before combat to grow the Bear that is about to attack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLifeTotal(2, 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInHand(1, "Little Bear")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $action — at 3 life, the Bear grown to 3/3 attacks for lethal") {
                (action as? CastSpell)?.let { game.state.getEntity(it.cardId)?.get<CardComponent>()?.name } shouldBe "Little Bear"
            }
        }
    }
}
