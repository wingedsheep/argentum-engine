package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Target creature you control gains protection from creatures your opponents control until end
 * of turn. Untap it." (Crypsis) — an opponents-scoped protection grant beside an untap of the
 * same creature. The untap is spent on resolution with the shield, so the whole spell reads as a
 * trick held for a fight, not a main-phase play into an empty board.
 */
class OpponentCreatureProtectionReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(category: PuzzleCategory, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "opponent-creature-protection",
                category = category,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    init {
        test("Crypsis reads as a shield on our creature that ends at cleanup") {
            val card = cardRegistry.requireCard("Crypsis")
            val reading = AiReadingAudit.read(card)
            withClue("$reading") { reading.readFully shouldBe true }
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.BENEFICIAL)
            val tags = CardIntentAnalyzer.analyze(card).tags
            withClue("a protection grant") { tags shouldContain IntentTag.PROTECTION }
            withClue("the untap rides on the shield: a trick to hold") { tags shouldContain IntentTag.COMBAT_TRICK }
        }

        test("Crypsis is not cast in our own main phase with nothing to fight") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s ->
                s.withPlayers()
                    .withActivePlayer(1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Crypsis")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Swamp") } }
                    .build()
            }) { shouldNotCast("Crypsis") }
            withClue("an until-end-of-turn shield is held for a fight: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
