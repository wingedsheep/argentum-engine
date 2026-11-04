package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An Aura whose only harm to its host is damage on entry beside a lasting gift — Tilonalli's
 * Crown's "it deals 1 damage to enchanted creature" and "+3/+0 and trample". On a creature that
 * survives the point it is a pump; on one that doesn't, removal. The slot reads FLEXIBLE, so the
 * simulation sees both sides of the table and picks the host by who survives.
 */
class DamageGiftAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "damage-gift-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim a damage-on-entry pump Aura by who survives it", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("a pump Aura that pings its host on entry is understood as either-way") {
            val crown = cardRegistry.getCard("Tilonalli's Crown")!!
            TargetPolarityAnalyzer.forSpell(crown) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(crown).readFully shouldBe true
        }

        test("Tilonalli's Crown goes on our creature that survives the damage, not on theirs") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Tilonalli's Crown")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .build()
            }) {
                shouldCast("Tilonalli's Crown")
                shouldTarget("Grizzly Bears")
            }
        }

    }
}
