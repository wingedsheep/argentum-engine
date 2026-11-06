package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Exile each creature with mana value of the chosen quality" (Extinction Event), "exile all
 * multicolored permanents" (Ravnica at War) lower to a gather of the battlefield moved to exile:
 * the wrath a destroy-all or bounce-all gather already reads as — removal, and a sweeper.
 */
class ExileAllPipelineReadingAiTest : ScenarioTestBase() {
    init {
        // Facing four creatures with one of its own, the AI pays a discard cost from a hand of a
        // wrath and a vanilla-ish 5-drop. Unread, the wrath scored as a plain 4-mana sorcery and
        // was the card pitched.
        test("a discard cost pitches the 5-drop, not the exile-all wrath") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(1, 6)
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Extinction Event")
                .withCardInHand(1, "Hollow Dogs")
                .withCardInHand(1, "Raise the Alarm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val hand = game.state.getHand(game.player1Id)
            fun named(name: String) = hand.first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }
            val wrath = named("Extinction Event")
            val dogs = named("Hollow Dogs")
            val legal = LegalAction(
                action = CastSpell(game.player1Id, named("Raise the Alarm")),
                actionType = "CastSpell",
                description = "test",
                additionalCostInfo = AdditionalCostData(
                    "discard", "DiscardCard", validDiscardTargets = listOf(wrath, dogs), discardCount = 1,
                ),
            )

            val chosen = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .chooseFrom(game.state, listOf(legal)).action as CastSpell
            withClue("an exile-all gather reads as a sweeper, so the wrath is worth keeping over a 3/3") {
                chosen.additionalCostPayment?.discardedCards shouldBe listOf(dogs)
            }
        }

        test("a gathered group moved to exile reads as a sweeper") {
            for (card in listOf("Extinction Event", "Ravnica at War", "Urza's Ruinous Blast")) {
                withClue(card) {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags shouldContainAll
                        listOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
                }
            }
        }

        // "Exile those tokens when Mysterio leaves the battlefield": the source taking back its own
        // creations is its drawback, not an answer.
        test("exiling the source's own creations is no sweeper") {
            for (card in listOf("Mysterio, Master of Illusion", "Dance of Many", "Tetravus")) {
                withClue(card) {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags shouldNotContain IntentTag.SWEEPER
                }
            }
        }
    }
}
