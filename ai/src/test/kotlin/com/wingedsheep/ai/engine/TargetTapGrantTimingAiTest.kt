package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A `{T}` ability handed to one target creature until end of turn — "target creature gains
 * '{T}: Return target nonland permanent to its owner's hand' until end of turn" (Retraction Helix).
 * It is spent only by that creature tapping for it before cleanup, so a cast onto a creature of ours
 * that is tapped or summoning sick wears off unused: the cast floor refuses it there, and keeps the
 * window open while the target can still tap.
 */
class TargetTapGrantTimingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun board(s: ScenarioBuilder, untapped: Boolean) = s.withPlayers()
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Retraction Helix")
        .withCardOnBattlefield(1, "Hill Giant", tapped = !untapped)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
        .withCardOnBattlefield(2, "Llanowar Elves")
        .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
        .build()

    init {
        test("Retraction Helix's single-target grant has an intent and a timing reading") {
            val helix = cardRegistry.requireCard("Retraction Helix")
            ExpiringGrantWindow.readsSpellActivationGrant(helix.script.spellEffect!!) shouldBe true
            CardIntentAnalyzer.analyzeEffect(helix.script.spellEffect!!).tags.isNotEmpty() shouldBe true
            AiReadingAudit.read(helix).readFully shouldBe true
        }

        test("the floor reads whether the targeted creature of ours can still tap") {
            val effect = cardRegistry.requireCard("Retraction Helix").script.spellEffect!!
            val intents = IntentCatalog.of(cardRegistry)
            for (untapped in listOf(true, false)) {
                val game = board(scenario(), untapped).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
                for ((name, buysNothing) in listOf("Hill Giant" to !untapped, "Grizzly Bears" to true, "Llanowar Elves" to true)) {
                    val target = ChosenTarget.Permanent(game.findPermanent(name)!!)
                    withClue("Hill Giant untapped = $untapped, target = $name") {
                        ExpiringGrantWindow.castBuysNothing(
                            game.state, game.player1Id, listOf(effect), listOf(target), intents, readKeywordGrants = true,
                        ) shouldBe buysNothing
                    }
                }
            }
        }

        test("a per-permanent grant fanned over a group is judged by the whole group, lands included") {
            // Flame Fusillade: "until end of turn, permanents you control gain '{T}: …'" runs the
            // single-target grant once per member; untapped lands can still spend it.
            val effect = cardRegistry.requireCard("Flame Fusillade").script.spellEffect!!
            val intents = IntentCatalog.of(cardRegistry)
            val game = board(scenario(), untapped = false).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            ExpiringGrantWindow.castBuysNothing(
                game.state, game.player1Id, listOf(effect), emptyList(), intents, readKeywordGrants = true,
            ) shouldBe false
        }

        test("Retraction Helix is not cast when no creature of ours could tap for it") {
            val result = runner.run(
                AiPuzzle(
                    id = "target-tap-grant-timing", category = PuzzleCategory.PRIORITY_TIMING,
                    expectation = "A granted {T} ability its creature can't tap for is a card thrown away", aiSeat = 1,
                    position = { s -> board(s, untapped = false).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) } },
                    check = { shouldNotCast("Retraction Helix") },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
