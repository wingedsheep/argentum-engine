package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An Aura whose trigger only reads its host's creature types to pick cards for its controller —
 * Call to the Kindred's "put a creature card that shares a creature type with enchanted creature
 * … onto the battlefield". The host names the tribe our own library is built around, so the enchant
 * slot is a gift on our side, and the "shares a creature type" filter rewards the deck's best tribe.
 */
class SharedTypeTutorAuraAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("Call to the Kindred's enchant slot reads beneficial and it pays off one tribe") {
            val card = cardRegistry.getCard("Call to the Kindred")!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(card).readFully shouldBe true
            SynergyAxes.of(card).rewardsChosenTribe shouldBe true
        }

        test("Call to the Kindred goes on our own creature, not the opponent's bigger one") {
            val result = runner.run(
                AiPuzzle(
                    id = "shared-type-tutor-aura", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "Enchant our own creature, whose type our library shares", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Island", 4)
                            .withCardInHand(1, "Call to the Kindred")
                            .withCardOnBattlefield(1, "Grizzly Bears")
                            .withCardOnBattlefield(2, "Craw Wurm")
                            .build()
                    },
                    check = { shouldCast("Call to the Kindred"); shouldTarget("Grizzly Bears") },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
