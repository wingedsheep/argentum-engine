package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class MustBeBlockedPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("forcing one blocker or every blocker does not fix the target's side") {
            for (all in listOf(false, true)) {
                val forceBlock = card("Forced block reader fixture") {
                    manaCost = "{G}"
                    typeLine = "Sorcery"
                    spell {
                        val creature = target(TargetFilter.Creature)
                        effect = Effects.MustBeBlocked(creature, allCreatures = all)
                    }
                }
                TargetPolarityAnalyzer.forSpell(forceBlock) shouldBe listOf(TargetPolarity.FLEXIBLE)
            }
            val growth = cardRegistry.requireCard("Emergent Growth")
            TargetPolarityAnalyzer.forSpell(growth) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(growth).readFully shouldBe true
        }

        test("Emergent Growth pumps our trampler before combat instead of an opposing blocker") {
            lateinit var position: TestGame
            val result = runner.run(
                AiPuzzle(
                    id = "must-be-blocked-pump", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "Consider our attacker despite the forced-block rider", aiSeat = 1,
                    position = { s ->
                        s.withPlayers().withActivePlayer(1)
                            .withLifeTotal(2, 9)
                            .withLandsOnBattlefield(1, "Forest", 4)
                            .withCardInHand(1, "Emergent Growth")
                            .withCardOnBattlefield(1, "Colossal Dreadmaw")
                            .withCardOnBattlefield(2, "Grizzly Bears")
                            .build().also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN); position = it }
                    },
                    check = {
                        shouldCast("Emergent Growth")
                        shouldTarget("Colossal Dreadmaw")
                        shouldNotTarget("Grizzly Bears")
                    },
                ),
                AiProfile.LIVE,
            )
            val insights = mutableListOf<com.wingedsheep.ai.insight.AiDecisionInsight>()
            if (!result.passed) AIPlayer.create(cardRegistry, position.player1Id, AiProfile.LIVE) { _, insight -> insights += insight }.chooseAction(position.state)
            val trace = insights.flatMap { it.options }.joinToString("\n") { "${it.label}: score=${it.score} ${it.note}" }
            withClue("chose ${result.move}: ${result.failure}\n$trace") { result.passed shouldBe true }
        }
    }
}
