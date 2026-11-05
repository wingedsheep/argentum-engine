package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Reveal the top three cards of your library. For each of those cards, put that card into your
 * hand unless any opponent pays 3 life" (Sword-Point Diplomacy): a for-each over our gathered
 * cards whose body puts each into our hand behind an opponents' punisher payment. Either branch is
 * ours — the card or their life — so it reads as [IntentTag.DRAW].
 */
class PunisherDigReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("a punisher dig into hand reads as card draw") {
            val diplomacy = cardRegistry.getCard("Sword-Point Diplomacy")!!
            withClue("each revealed card goes to our hand unless an opponent pays life") {
                CardIntentAnalyzer.analyze(diplomacy).tags shouldContain IntentTag.DRAW
                AiReadingAudit.audit(diplomacy).shouldBeEmpty()
            }
        }

        test("the punisher dig is cast with idle mana") {
            val result = runner.run(
                AiPuzzle(
                    id = "punisher-dig",
                    category = PuzzleCategory.PRIORITY_TIMING,
                    expectation = "",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(1)
                            .withLandsOnBattlefield(1, "Swamp", 3)
                            .withCardInHand(1, "Sword-Point Diplomacy")
                            .apply { repeat(12) { withCardInLibrary(1, "Kulrath Zealot") } }
                            .apply { repeat(12) { withCardInLibrary(2, "Island") } }
                            .build()
                            .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
                    },
                    check = { shouldCast("Sword-Point Diplomacy") },
                ),
                AiProfile.LIVE,
            )
            withClue("cards or the opponent's life, either is ours: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
