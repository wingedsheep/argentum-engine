package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Enchanted creature attacks each combat if able" (Skin Invasion) takes away its controller's
 * choice to hold the creature back, so it runs into the blockers of whoever cast the Aura: on its
 * own it is a curse, [TargetPolarity.HARMFUL], and the Aura goes on the opponent's creature.
 * Beside a gift ("gets +2/+2 and attacks each combat if able", Furor of the Bitten) it is the
 * gift's price, not a veto on it, and the Aura stays a pump for our own creature.
 *
 * Unread, Skin Invasion's slot was UNKNOWN and left to the fallbacks; the puzzle pins the placement
 * the reading now states outright.
 */
class ForcedAttackAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("a forced attack alone curses the host; beside a pump it is the pump's price") {
            TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Skin Invasion")!!) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Furor of the Bitten")!!) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(cardRegistry.getCard("Skin Invasion")!!).readFully shouldBe true
        }

        test("Skin Invasion goes on the opponent's creature, not on our own") {
            val result = runner.run(
                AiPuzzle(
                    id = "forced-attack-aura", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "Force the opponent's creature to attack", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Mountain", 1)
                            .withCardInHand(1, "Skin Invasion")
                            .withCardOnBattlefield(1, "Grizzly Bears")
                            .withCardOnBattlefield(2, "Hill Giant")
                            .build()
                    },
                    check = {
                        shouldCast("Skin Invasion")
                        shouldTarget("Hill Giant")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
