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
 * An Aura whose only ability is its host's combat trigger — "whenever enchanted creature attacks,
 * it gets +X/+0 until end of turn" (Thunderous Might). The attached trigger binds the host as the
 * triggering entity, so the pump on "it" is a gift to the creature the Aura enchants: the Aura goes
 * on our creature, not the opponent's.
 */
class AttachedAttackTriggerAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("Thunderous Might's enchant slot reads as a gift") {
            val might = cardRegistry.requireCard("Thunderous Might")
            TargetPolarityAnalyzer.forSpell(might) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(might).readFully shouldBe true
        }

        test("Thunderous Might goes on our creature, not on the opponent's") {
            val result = runner.run(
                AiPuzzle(
                    id = "attached-attack-trigger-aura", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "An attack-trigger pump Aura enchants our own creature", aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Mountain", 2)
                            .withCardInHand(1, "Thunderous Might")
                            .withCardOnBattlefield(1, "Grizzly Bears")
                            .withCardOnBattlefield(2, "Hill Giant")
                            .build()
                    },
                    check = {
                        shouldCast("Thunderous Might")
                        shouldTarget("Grizzly Bears")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
