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
 * An Aura granting its host "Untap this permanent during each other player's untap step" —
 * Urban Burgeoning's enchanted land. The host's controller gets the untap on every other turn, so
 * the enchant slot is a gift: the Aura goes on our land, not the opponent's.
 */
class GrantedSelfUntapAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("Urban Burgeoning's enchant slot reads as a gift") {
            val burgeoning = cardRegistry.requireCard("Urban Burgeoning")
            TargetPolarityAnalyzer.forSpell(burgeoning) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(burgeoning).readFully shouldBe true
        }

        test("Urban Burgeoning goes on our land, not on the opponent's") {
            val result = runner.run(
                AiPuzzle(
                    id = "granted-self-untap-aura", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "A self-untap Aura enchants our own land", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Forest", 1)
                            .withCardInHand(1, "Urban Burgeoning")
                            .withLandsOnBattlefield(2, "Island", 1)
                            .build()
                    },
                    check = {
                        shouldCast("Urban Burgeoning")
                        shouldTarget("Forest")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
