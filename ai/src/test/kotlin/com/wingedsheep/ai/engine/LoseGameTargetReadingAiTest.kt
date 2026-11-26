package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Target player loses the game" (Door to Nothingness) is only ever aimed at an opponent, so its
 * player slot reads [TargetPolarity.HARMFUL]. Unread, the slot was an unknown in the reading audit
 * and the AI had no prior against pointing the Door at itself.
 */
class LoseGameTargetReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("a target player who loses the game reads harmful to that player") {
            val ability = cardRegistry.getCard("Door to Nothingness")!!.activatedAbilities.single()
            intents.abilityTargetPolarities("Door to Nothingness", ability.id) shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("the AI opens Door to Nothingness on its opponent") {
            val result = runner.run(
                AiPuzzle(
                    id = "door-to-nothingness",
                    category = PuzzleCategory.PRIORITY_TIMING,
                    expectation = "",
                    aiSeat = 1,
                    position = { s ->
                        listOf("Plains", "Island", "Swamp", "Mountain", "Forest")
                            .fold(s.withPlayers().withTurnNumber(6)) { b, land -> b.withLandsOnBattlefield(1, land, 2) }
                            .withCardOnBattlefield(1, "Door to Nothingness")
                            .withLandsOnBattlefield(2, "Forest", 3)
                            .let { b -> (1..10).fold(b) { acc, _ -> acc.withCardInLibrary(1, "Plains").withCardInLibrary(2, "Forest") } }
                            .build()
                    },
                    check = {
                        shouldActivate("Door to Nothingness")
                        shouldTargetOpponentFace()
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
    }
}
