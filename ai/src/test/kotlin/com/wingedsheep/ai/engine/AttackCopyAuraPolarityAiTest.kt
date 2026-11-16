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
 * An Aura that copies its host whenever the host attacks — Mirror Mockery's "you may create a
 * token that's a copy of that creature. Exile that token at end of combat" — hands its controller
 * an untapped, non-attacking copy mid-attack: a blocker made of the opponent's own attacker. Its
 * enchant slot reads HARMFUL, aimed at the opponent's creature.
 */
class AttackCopyAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "attack-copy-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Put a copy-on-attack Aura on the opponent's attacker", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("Mirror Mockery's enchant slot reads harmful and is read fully") {
            val card = cardRegistry.getCard("Mirror Mockery")!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            AiReadingAudit.read(card).readFully shouldBe true
        }

        test("Mirror Mockery goes on the opponent's Hill Giant, not our Grizzly Bears") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Mirror Mockery")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Mirror Mockery")
                shouldTarget("Hill Giant")
            }
        }
    }
}
