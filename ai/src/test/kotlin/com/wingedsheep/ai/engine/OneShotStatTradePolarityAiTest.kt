package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A fixed one-shot stat trade — Spatial Contortion's "target creature gets +3/-3 until end of turn" —
 * kills a small creature of theirs as often as it turns a sturdy one of ours into a bigger attacker.
 * Like the lasting trade on an Aura it reads flexible, so the simulation picks the side; unread, the
 * slot fell back to a ranking that knew nothing of the -3.
 */
class OneShotStatTradePolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("a fixed +N/-M until end of turn reads flexible and fully") {
            val contortion = cardRegistry.requireCard("Spatial Contortion")
            TargetPolarityAnalyzer.forSpell(contortion) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(contortion).readFully shouldBe true
        }

        test("Spatial Contortion kills their lethal 3/3 attacker rather than our 2/2") {
            val result = runner.run(
                AiPuzzle(
                    id = "one-shot-stat-trade", category = PuzzleCategory.REMOVAL_TARGETING,
                    expectation = "+3/-3 aims at the creature it kills on their side", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(2)
                            .withLandsOnBattlefield(1, "Wastes", 2)
                            .withCardInHand(1, "Spatial Contortion")
                            // Tapped, so it can't block: the 3/3 attacker is lethal unless it dies.
                            .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                            .withLifeTotal(1, 3)
                            .withCardOnBattlefield(2, "Hill Giant")
                            .build()
                            .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                            .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                            // A combat trick too, so it is held until blocks are in.
                            .advanceToPriority(1, Step.DECLARE_BLOCKERS)
                    },
                    check = {
                        shouldCast("Spatial Contortion")
                        shouldTarget("Hill Giant")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
