package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Return up to two target creatures your opponents control to their owners' hands. Target player
 * draws two cards." (Roiling Waters) runs its bounce once per target of every slot. Each slot reads
 * as the body reads on it alone — the creatures as a bounce, the player (whom a bounce can't move)
 * from the draw — where before a second slot left the creature slot unread. The puzzle pins the
 * play the reading must keep: their creature bounced, our own player drawing.
 */
class MultiSlotForEachTargetPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("a per-target bounce beside a player slot reads every slot") {
            val waters = cardRegistry.requireCard("Roiling Waters")
            TargetPolarityAnalyzer.forSpell(waters) shouldNotContain TargetPolarity.UNKNOWN
            AiReadingAudit.read(waters).readFully shouldBe true
        }

        test("Roiling Waters bounces their biggest creature and draws us the cards") {
            val result = runner.run(
                AiPuzzle(
                    id = "multi-slot-for-each-target", category = PuzzleCategory.REMOVAL_TARGETING,
                    expectation = "Bounce their dragon, draw ourselves", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Island", 7)
                            .withCardInHand(1, "Roiling Waters")
                            // Deep libraries on both sides, so the draw is card advantage, not a way to deck them.
                            .apply { repeat(12) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                            .withCardOnBattlefield(2, "Serra Angel")
                            .withCardOnBattlefield(2, "Shivan Dragon")
                            .build()
                    },
                    check = {
                        shouldCast("Roiling Waters")
                        shouldTarget("Shivan Dragon")
                        shouldTarget("you")
                        shouldNotTarget("the opponent")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
