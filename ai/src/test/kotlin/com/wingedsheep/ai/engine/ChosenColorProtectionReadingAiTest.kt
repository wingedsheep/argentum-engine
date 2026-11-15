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
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Choose a color, then target creature you control gains protection from it until end of turn"
 * ([com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect] around
 * [com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect]) — Center Soul, Gods
 * Willing. The color choice only parameterises the grant, so the readers see through it to the
 * shield: a beneficial target, [IntentTag.PROTECTION], and — since it all ends at cleanup — a
 * trick held for removal on the stack rather than fired into an empty main phase.
 */
class ChosenColorProtectionReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(category: PuzzleCategory, position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "chosen-color-protection",
                category = category,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    private fun board(s: ScenarioBuilder, active: Int) = s.withPlayers()
        .withActivePlayer(active)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Serra Angel")
        .withCardInHand(1, "Center Soul")
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardInHand(2, "Murder")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("the color choice is read through to the protection grant it wraps") {
            for (name in listOf("Center Soul", "Gods Willing")) {
                val card = cardRegistry.requireCard(name)
                val reading = AiReadingAudit.read(card)
                withClue("$name: $reading") { reading.readFully shouldBe true }
                withClue("$name's target is helped") {
                    TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
                val tags = CardIntentAnalyzer.analyze(card).tags
                withClue("$name is a shield") { tags shouldContain IntentTag.PROTECTION }
                withClue("$name ends at cleanup: a trick to hold") { tags shouldContain IntentTag.COMBAT_TRICK }
            }
            val mother = cardRegistry.requireCard("Mother of Runes")
            val reading = AiReadingAudit.read(mother)
            withClue("Mother of Runes' tap-for-protection knows its window: $reading") { reading.readFully shouldBe true }
        }

        test("Center Soul is not cast in our own main phase with nothing to protect against") {
            val result = decide(PuzzleCategory.PRIORITY_TIMING, { s -> board(s, 1) }) { shouldNotCast("Center Soul") }
            withClue("an until-end-of-turn shield is a response, not a main-phase play: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("Center Soul answers removal on our creature") {
            val result = decide(PuzzleCategory.STACK_RESPONSE, { s ->
                board(s, 2).also { g ->
                    g.castSpell(2, "Murder", g.findPermanent("Serra Angel"))
                    g.advanceToStackResponse(1)
                }
            }) {
                shouldCast("Center Soul")
                shouldTarget("Serra Angel")
            }
            withClue("the shield is spent in response to removal: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
