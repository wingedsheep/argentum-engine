package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A `{T}` ability handed to a group of ours until end of turn — "until end of turn, creatures you
 * control gain '{T}: This creature deals 1 damage to any target'" (Lightning Volley). It is spent
 * only by a member tapping for it before cleanup, so a cast with every creature of ours tapped or
 * summoning sick wears off unused: the cast floor refuses it there, and keeps the window open
 * while one creature can still tap.
 */
class GroupTapGrantTimingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun board(s: ScenarioBuilder, untapped: Boolean) = s.withPlayers()
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInHand(1, "Lightning Volley")
        .withCardOnBattlefield(1, "Hill Giant", tapped = !untapped)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
        .withCardOnBattlefield(2, "Llanowar Elves")
        .apply { repeat(4) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Forest") } }
        .build()

    init {
        test("Lightning Volley's group grant has a timing reading") {
            val volley = cardRegistry.requireCard("Lightning Volley")
            ExpiringGrantWindow.readsSpellActivationGrant(volley.script.spellEffect!!) shouldBe true
            AiReadingAudit.read(volley).readFully shouldBe true
        }

        test("the floor reads whether any creature of ours can still tap") {
            val effect = cardRegistry.requireCard("Lightning Volley").script.spellEffect!!
            val intents = IntentCatalog.of(cardRegistry)
            for (untapped in listOf(true, false)) {
                val game = board(scenario(), untapped).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
                withClue("an untapped, non-sick creature = $untapped") {
                    ExpiringGrantWindow.castBuysNothing(
                        game.state, game.player1Id, listOf(effect), emptyList(), intents, readKeywordGrants = true,
                    ) shouldBe !untapped
                }
            }
        }

        test("Lightning Volley is not cast when every creature of ours is tapped or summoning sick") {
            val result = runner.run(
                AiPuzzle(
                    id = "group-tap-grant-timing", category = PuzzleCategory.PRIORITY_TIMING,
                    expectation = "A granted {T} ability nothing can tap for is a card thrown away", aiSeat = 1,
                    position = { s -> board(s, untapped = false).also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) } },
                    check = { shouldNotCast("Lightning Volley") },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
