package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
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

/**
 * Two target readings:
 *
 * - "Exile target creature and all other creatures its controller controls with the same name"
 *   (Declaration in Stone) as a pipeline: the stored target, and the permanents its controller
 *   controls, are exiled. That is removal, so it goes on the opponent's creature.
 * - "Enchanted creature has 'whenever this creature attacks, create a 4/4 Angel that's tapped and
 *   attacking. Exile that token at end of combat'" (Invocation of Saint Traft): the delayed exile
 *   only takes back the token the trigger made, so the granted trigger is a gift. It goes on our
 *   creature, and the Angel feeds a tokens deck.
 */
class StoredTargetExileAndDelayedPricePolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "stored-target-exile-delayed-price", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim removal at their creature and a gift Aura at ours", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("the readings: a pipelined exile is harmful, a granted trigger with a take-back price is helpful") {
            val declaration = cardRegistry.getCard("Declaration in Stone")!!
            val invocation = cardRegistry.getCard("Invocation of Saint Traft")!!
            TargetPolarityAnalyzer.forSpell(declaration) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(invocation) shouldBe listOf(TargetPolarity.BENEFICIAL)
            // The Angel the granted attack trigger makes is a token of ours, as if the trigger were printed.
            SynergyAxes.of(invocation).feeds shouldContain SynergyAxis.TOKENS
            for (card in listOf(declaration, invocation)) {
                withClue(card.name) { AiReadingAudit.read(card).readFully shouldBe true }
            }
        }

        test("Declaration in Stone exiles the opponent's creature, not our own") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Declaration in Stone")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Declaration in Stone")
                shouldTarget("Hill Giant")
            }
        }

        test("Invocation of Saint Traft goes on our creature, not on the opponent's") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Invocation of Saint Traft")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Invocation of Saint Traft")
                shouldTarget("Grizzly Bears")
            }
        }
    }
}
