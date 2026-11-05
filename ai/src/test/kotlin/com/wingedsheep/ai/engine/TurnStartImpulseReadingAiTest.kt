package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A "this turn" impulse made by a trigger that fires on our own turn with a main phase still
 * ahead — "at the beginning of your upkeep, exile the top card of your library … you may cast that
 * card this turn" (Vance's Blasting Cannons), "at the beginning of your second main phase … until
 * end of turn, you may play that card" (Fireglass Mentor), "whenever this attacks" (Dragonhawk,
 * Fate's Tempest), "whenever this deals combat damage to a player" — has a main phase left to
 * spend the card in, as a card drawn that turn does. It reads as [IntentTag.DRAW], and the permanent
 * is priced as a repeatable card-draw engine on the battlefield.
 *
 * A "this turn" impulse we time ourselves (Act on Impulse), or one on a trigger that can fire on an
 * opponent's turn (Harnesser of Storms' cast trigger, Emberheart Challenger's valiant), still does
 * not read as one.
 */
class TurnStartImpulseReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")

    init {
        test("an upkeep impulse is card draw, and the permanent is priced as repeatable draw") {
            val cannons = card("Vance's Blasting Cannons")
            val intent = CardIntentAnalyzer.analyzeSelf(cannons)
            withClue("the upkeep exile-and-cast-this-turn reads as a draw") {
                intent.tags shouldContain IntentTag.DRAW
                AiReadingAudit.audit(cannons).shouldBeEmpty()
            }
            withClue("on the battlefield it is worth repeatable card draw, not a generic repeatable ability") {
                CardIntentAnalyzer.priorLadder(intent).map { it.claim } shouldContain "repeatable card draw"
            }
        }

        test("a second-main-phase or attack-trigger impulse is card draw too") {
            listOf("Fireglass Mentor", "Dragonhawk, Fate's Tempest").forEach { name ->
                val intent = CardIntentAnalyzer.analyzeSelf(card(name))
                withClue("$name's this-turn impulse fires on our turn with a main phase ahead") {
                    intent.tags shouldContain IntentTag.DRAW
                }
                withClue("$name is a creature that keeps drawing: its ability is worth more than an unread one") {
                    intent.staticPriorValue shouldBeGreaterThan CardIntent.UNKNOWN.staticPriorValue
                }
            }
            withClue("Fireglass Mentor has nothing else to read") {
                AiReadingAudit.audit(card("Fireglass Mentor")).shouldBeEmpty()
            }
        }

        test("an until-your-next-end-step impulse is card draw from any trigger, entering included") {
            val dragonhawk = card("Dragonhawk, Fate's Tempest")
            withClue("both the enters and the attack trigger read fully") {
                AiReadingAudit.audit(dragonhawk).shouldBeEmpty()
            }
        }

        test("a this-turn impulse we time ourselves, or one that can fire on their turn, still is not read as a draw") {
            CardIntentAnalyzer.analyze(card("Act on Impulse")).tags shouldNotContain IntentTag.DRAW
            CardIntentAnalyzer.analyzeSelf(card("Harnesser of Storms")).tags shouldNotContain IntentTag.DRAW
        }

        test("Lightning Bolt kills the card-draw engine, not the vanilla creature beside it") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(5)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(2, "Fireglass Mentor")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val mentor = game.findPermanent("Fireglass Mentor")!!

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $action — Fireglass Mentor hands its controller a card every turn they hit us; Grizzly Bears is a 2/2") {
                (action as? CastSpell)?.targets shouldBe listOf(ChosenTarget.Permanent(mentor))
            }
        }
    }
}
