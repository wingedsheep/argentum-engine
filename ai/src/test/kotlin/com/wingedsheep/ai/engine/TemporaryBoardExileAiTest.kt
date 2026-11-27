package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Exile all nonland permanents target player controls. Return the exiled cards … at the beginning
 * of the next end step" (Sudden Disappearance): one player's board taken away for the turn. The
 * player slot is that player's loss, and the intent is removal that comes back — never a wrath or
 * an exile for good.
 */
class TemporaryBoardExileAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("Sudden Disappearance hurts its target player and reads as temporary removal") {
            val card = cardRegistry.getCard("Sudden Disappearance")!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            AiReadingAudit.read(card).readFully shouldBe true
            val tags = CardIntentAnalyzer.analyze(card).tags
            tags shouldContain IntentTag.REMOVAL
            tags shouldNotContain IntentTag.SWEEPER
            tags shouldNotContain IntentTag.EXILE_REMOVAL
        }

        test("Sudden Disappearance is aimed at the opponent, clearing their blockers for our attack") {
            val result = runner.run(
                AiPuzzle(
                    id = "temporary-board-exile", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "Exile the opponent's board, not our own", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLifeTotal(2, 4)
                            .withLandsOnBattlefield(1, "Plains", 6)
                            .withCardInHand(1, "Sudden Disappearance")
                            .withCardOnBattlefield(1, "Hill Giant")
                            .withCardOnBattlefield(1, "Grizzly Bears")
                            .withCardOnBattlefield(2, "Craw Wurm")
                            .build()
                    },
                    check = { shouldCast("Sudden Disappearance"); shouldTargetOpponentFace() },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
