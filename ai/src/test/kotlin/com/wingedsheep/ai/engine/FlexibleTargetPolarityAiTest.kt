package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Known two-sided effects must reach simulation with both friendly and opposing candidates. */
class FlexibleTargetPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "flexible-target-polarity", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Compare both sides of a flexible target slot", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("bounce and unrestricted blink are understood without committing to a target side") {
            for (name in listOf("Aether Burst", "Grasping Current", "Flicker of Fate", "Unsummon")) {
                val card = cardRegistry.getCard(name)!!
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
                    AiReadingAudit.read(card).abilities.flatMap { it.findings }
                        .filter { it.check == AiReadingAudit.Check.POLARITY && !it.read } shouldBe emptyList()
                }
            }
        }

        test("a flexible operation stays understood beside known harm or benefit") {
            // Exile then return-to-hand in one branch, plain bounce in the other: the harmful
            // exile is understood, and must not turn the flexible return into a mixed gap.
            val excavation = cardRegistry.getCard("Meticulous Excavation")!!
            val ability = excavation.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(excavation, ability) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(excavation).abilities.flatMap { it.findings }
                .filter { it.check == AiReadingAudit.Check.POLARITY && !it.read } shouldBe emptyList()

            val blinkThenBounce = card("Flexible blink and bounce reader fixture") {
                manaCost = "{U}"
                typeLine = "Instant"
                spell {
                    val creature = target(TargetFilter.CreatureYouControl)
                    effect = Effects.Exile(creature) then Effects.Move(creature, Zone.BATTLEFIELD) then
                        Effects.ReturnToHand(creature)
                }
            }
            TargetPolarityAnalyzer.forSpell(blinkThenBounce) shouldBe listOf(TargetPolarity.FLEXIBLE)
        }

        test("a flexible operation does not hide an unread move on the same target") {
            val unread = card("Flexible plus unread move reader fixture") {
                manaCost = "{U}"
                typeLine = "Instant"
                spell {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.ReturnToHand(creature) then Effects.Move(creature, Zone.COMMAND)
                }
            }
            TargetPolarityAnalyzer.forSpell(unread) shouldBe listOf(TargetPolarity.UNKNOWN)
            AiReadingAudit.read(unread).abilities.flatMap { it.findings }
                .any { it.check == AiReadingAudit.Check.POLARITY && !it.read } shouldBe true
        }

        test("Aether Burst saves our Angel from Murder instead of freeing their pacified Bear") {
            decide({ s ->
                s.withPlayers().withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInHand(2, "Murder")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    // Bouncing this legal opposing target frees it from our removal Aura.
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(1, "Aether Burst")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .build().also {
                        it.castSpell(2, "Murder", it.findPermanent("Serra Angel")).error shouldBe null
                    }.advanceToStackResponse(1)
            }) {
                shouldCast("Aether Burst")
                shouldTarget("Serra Angel")
            }
        }

        test("Flicker of Fate removes their token when every legal target is opposing") {
            decide({ s ->
                s.withPlayers().withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(2, "Serra Angel", isToken = true)
                    .build().advanceToPriority(1, Step.END)
            }) {
                shouldCast("Flicker of Fate")
                shouldTarget("Serra Angel")
            }
        }

        test("Grasping Current still bounces opposing creatures offensively") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInHand(1, "Grasping Current")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Serra Angel", isToken = true)
                    .withCardOnBattlefield(2, "Shivan Dragon", isToken = true)
                    .build()
            }) {
                shouldCast("Grasping Current")
                shouldTarget("Serra Angel")
                shouldTarget("Shivan Dragon")
            }
        }
        test("Grasping Current leaves its spare target unused instead of bouncing our creature") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInHand(1, "Grasping Current")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(2, "Shivan Dragon", isToken = true)
                    .build()
            }) {
                shouldCast("Grasping Current")
                shouldTarget("Shivan Dragon")
                shouldNotTarget("Serra Angel")
            }
        }
    }
}
