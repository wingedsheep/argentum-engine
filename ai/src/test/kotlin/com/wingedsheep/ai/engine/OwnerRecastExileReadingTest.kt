package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Exile target permanent. Its owner may cast it from exile" — Airbend, Release to the Wind,
 * Suspend Aggression. The recast goes back to the card's owner, so the exile is a bounce that
 * skips the hand: tempo removal on their permanent, a save or an ETB re-buy on ours. It reads as a
 * bounce does — a flexible slot and removal intent — and simulation picks the side. A grant to the
 * caster instead (Abstruse Appropriation's "you may cast that card") is a steal, not a bounce.
 */
class OwnerRecastExileReadingTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "owner-recast-exile", category = PuzzleCategory.REMOVAL_TARGETING,
                expectation = "Aim an owner-recast exile at the opponent's best permanent", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("an exile its owner may recast reads as a bounce") {
            for (name in listOf("Release to the Wind", "Suspend Aggression", "Airbending Lesson")) {
                val card = cardRegistry.getCard(name)!!
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
                    CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.REMOVAL
                    AiReadingAudit.read(card).abilities.flatMap { it.findings }.filterNot { it.read } shouldBe emptyList()
                }
            }
        }

        test("an exile the caster may cast is not read as a bounce") {
            val steal = cardRegistry.getCard("Abstruse Appropriation")!!
            TargetPolarityAnalyzer.forSpell(steal) shouldNotBe listOf(TargetPolarity.FLEXIBLE)
        }

        test("Release to the Wind sends away their Dragon rather than our Bears") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Release to the Wind")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .build()
            }) {
                shouldCast("Release to the Wind")
                shouldTarget("Shivan Dragon")
            }
        }
    }
}
