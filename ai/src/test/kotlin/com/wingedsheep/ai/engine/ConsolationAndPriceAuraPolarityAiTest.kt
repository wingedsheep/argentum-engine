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

/**
 * Auras whose abilities pull both ways, read by which one is the point:
 *
 * - A curse that grants its host an activated ability (Compulsory Rest's "can't attack or block" and
 *   "{2}, Sacrifice this creature: You gain 2 life") is still a curse — the granted ability is only
 *   the consolation its controller may take. It goes on the opponent's creature.
 * - A gift that grants its host a harmful trigger (Consuming Fervor's "+3/+3" and "at the beginning
 *   of your upkeep, put a -1/-1 counter on this creature") is a pump that fades: the trigger is its
 *   price. It goes on our creature.
 *
 * Alongside, "target creature an opponent controls doesn't untap during its controller's next untap
 * step" (Kefnet's Monument) reads as a tapper, and an untap of "another target creature/permanent"
 * (Samut, Voice of Dissent; Vizier of Tumbling Sands) as an untap of our own.
 */
class ConsolationAndPriceAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "consolation-price-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim a two-sided Aura by which half is the point", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("the readings: a curse with a consolation, a gift with a price, a lock-down, an untap") {
            val rest = cardRegistry.getCard("Compulsory Rest")!!
            val fervor = cardRegistry.getCard("Consuming Fervor")!!
            TargetPolarityAnalyzer.forSpell(rest) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(fervor) shouldBe listOf(TargetPolarity.BENEFICIAL)

            val monument = cardRegistry.getCard("Kefnet's Monument")!!
            val lock = monument.script.triggeredAbilities.single()
            CardIntentAnalyzer.analyzeEffect(lock.effect, listOfNotNull(lock.targetRequirement)).tags shouldContain
                IntentTag.TAPPER

            for (name in listOf("Compulsory Rest", "Consuming Fervor", "Kefnet's Monument", "Samut, Voice of Dissent", "Vizier of Tumbling Sands")) {
                withClue(name) { AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true }
            }
        }

        test("Compulsory Rest goes on the opponent's attacker, not on our own creature") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Compulsory Rest")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Compulsory Rest")
                shouldTarget("Hill Giant")
            }
        }

        test("Consuming Fervor goes on our creature, not on the opponent's") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Consuming Fervor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Consuming Fervor")
                shouldTarget("Grizzly Bears")
            }
        }
    }
}
