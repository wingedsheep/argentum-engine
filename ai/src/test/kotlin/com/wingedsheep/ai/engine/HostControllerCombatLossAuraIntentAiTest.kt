package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Whenever enchanted creature attacks or blocks, its controller loses 2 life" (Sinister
 * Possession, Contaminated Bond): a harmful trigger the Aura hands its host, whose `you` is the
 * host's controller. The Aura's polarity already puts it on an opponent's creature; what it is
 * *for* is the life that opponent loses on every attack or block — [IntentTag.DAMAGE_TO_PLAYERS].
 *
 * Unread, the Aura had no intent at all ("abilities not recognised").
 */
class HostControllerCombatLossAuraIntentAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a granted 'its controller loses life' trigger on the host drains its controller") {
            for (name in listOf("Sinister Possession", "Contaminated Bond")) {
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(card(name)) shouldBe listOf(TargetPolarity.HARMFUL)
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
                }
            }
        }

        test("Sinister Possession goes on the opponent's creature, not our own") {
            val result = runner.run(
                AiPuzzle(
                    id = "host-controller-combat-loss-aura", category = PuzzleCategory.LAST_CHANCE,
                    expectation = "Curse the opponent's attacker with the life-loss Aura", aiSeat = 1,
                    position = { s ->
                        s.withPlayers().withActivePlayer(1)
                            .withLandsOnBattlefield(1, "Swamp", 1)
                            .withCardInHand(1, "Sinister Possession")
                            .withCardOnBattlefield(1, "Grizzly Bears")
                            .withCardOnBattlefield(2, "Hill Giant")
                            .build().also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
                    },
                    check = {
                        shouldCast("Sinister Possession")
                        shouldTarget("Hill Giant")
                        shouldNotTarget("Grizzly Bears")
                    },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
