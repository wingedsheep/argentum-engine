package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * What a permanent already on the battlefield can still answer.
 *
 * - An ability activated only from another zone ("{1}{W}, Discard this card: it deals 4 damage to
 *   target attacking or blocking creature", Gideon's Memorial) is gone once the card is in play.
 *   Read whole, it made the Memorial "repeatable removal" (4.0), the most valuable thing a
 *   noncreature permanent can be, above a repeatable tapper.
 * - "Discard a card: draw a card" (Murmuring Volume, Solitary Cell) trades card for card: a filter,
 *   not the repeatable card draw (3.0) it read as.
 * - "Whenever equipped creature attacks, you may sacrifice this Equipment. If you do, it deals 2
 *   damage to any target" (Throwing Knife) answers once: its equip recurs, its removal doesn't.
 * - Exiling a permanent *you control* (Identity Echo's polymorph) takes away our own creature: the
 *   price of what follows, not removal.
 */
class InPlayRemovalReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("on the battlefield, a spent channel artifact is worth less to the evaluator than an Icy Manipulator") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Gideon's Memorial")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .build()
            val intents = IntentCatalog.of(cardRegistry)
            fun value(name: String): Double {
                val id = game.findPermanent(name)!!
                val component = game.state.getEntity(id)!!.get<CardComponent>()!!
                return BoardPresence.permanentValue(
                    game.state, game.state.projectedState, id, component, intents, AiProfile.LIVE.creatureValuation,
                )
            }
            withClue("Gideon's Memorial's discard ability works only from the hand: in play it is a token anthem, no removal") {
                value("Gideon's Memorial") shouldBeLessThan value("Icy Manipulator")
            }
        }

        test("an Equipment that sacrifices itself to deal its damage is one-shot removal, not repeatable") {
            val knife = CardIntentAnalyzer.analyzeInPlay(card("Throwing Knife"))
            knife.tags shouldContain IntentTag.REMOVAL
            knife.repeatableTags shouldNotContain IntentTag.REMOVAL
            withClue("priced below the 4.0 repeatable-removal rung") { knife.staticPriorValue shouldBeLessThan 3.5 }
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Throwing Knife")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .build()
            val intents = IntentCatalog.of(cardRegistry)
            fun value(name: String): Double {
                val id = game.findPermanent(name)!!
                val component = game.state.getEntity(id)!!.get<CardComponent>()!!
                return BoardPresence.permanentValue(
                    game.state, game.state.projectedState, id, component, intents, AiProfile.LIVE.creatureValuation,
                )
            }
            value("Throwing Knife") shouldBeLessThan value("Icy Manipulator")
        }

        test("an ability activated only from the hand gives a permanent in play no removal prior") {
            val memorial = CardIntentAnalyzer.analyzeInPlay(card("Gideon's Memorial"))
            memorial.repeatable shouldBe false
            memorial.staticPriorValue shouldBeLessThan 3.5
        }

        test("exiling a permanent we control is not removal") {
            val echo = CardIntentAnalyzer.analyze(card("Identity Echo"))
            echo.tags shouldNotContain IntentTag.REMOVAL
            echo.tags shouldNotContain IntentTag.EXILE_REMOVAL
            CardIntentAnalyzer.analyzeInPlay(card("Identity Echo")).staticPriorValue shouldBeLessThan 3.5
        }

        test("discarding a card to draw a card is card selection, not a card-draw engine") {
            for (name in listOf("Murmuring Volume", "Solitary Cell")) {
                val intent = CardIntentAnalyzer.analyzeInPlay(card(name))
                withClue(name) {
                    intent.tags shouldNotContain IntentTag.DRAW
                    intent.tags shouldContain IntentTag.CARD_SELECTION
                    intent.staticPriorValue shouldBeLessThan 3.0
                    SynergyAxes.of(card(name)).feeds shouldContain SynergyAxis.GRAVEYARD
                }
            }
        }

        test("exiling a creature we control and returning it under our control is a blink, not removal") {
            for (name in listOf("Cloudshift", "Nephalia Smuggler", "Thassa, Deep-Dwelling", "Don & Leo, Problem Solvers")) {
                val tags = CardIntentAnalyzer.analyze(card(name)).tags
                withClue(name) {
                    tags shouldNotContain IntentTag.REMOVAL
                    tags shouldContain IntentTag.PROTECTION
                }
            }
        }
    }
}
