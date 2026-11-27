package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Three Avacyn Restored Auras-and-attachments the readers got wrong:
 *
 * - A steal Aura that also buffs (Spirit Away's "You control enchanted creature. Enchanted creature
 *   gets +2/+2 and has flying") is a theft: the buffs land on the creature once it is ours, so they
 *   never veto the steal. It goes on the opponent's best creature.
 * - "Prevent all damage that would be dealt by enchanted creature" (Defang) is a Pacifism — the
 *   polarity reader already aimed it, but the intent read nothing.
 * - "Attach to it any number of Auras on the battlefield" (Bruna, Light of Alabaster) runs through
 *   the free-equip attach leaf, but it moves Auras: no Equipment-mover read, and it asks the deck
 *   for Auras, not Equipment.
 */
class StealAuraAndAuraMoverReadingAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "steal-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim a buffing steal Aura at the opponent's creature", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("the readings: a buffing steal is a theft, a host-damage prevention neutralizes") {
            TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Spirit Away")!!) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Defang")!!) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyze(cardRegistry.getCard("Defang")!!).tags shouldContain IntentTag.NEUTRALIZE
            for (name in listOf("Spirit Away", "Defang", "Bruna, Light of Alabaster")) {
                withClue(name) { AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true }
            }
        }

        test("Bruna moves Auras, not Equipment, and asks the deck for Auras") {
            val bruna = cardRegistry.getCard("Bruna, Light of Alabaster")!!
            CardIntentAnalyzer.analyze(bruna).tags shouldNotContain IntentTag.EQUIPMENT_RELOCATION
            val asks = SynergyAxes.of(bruna).asks.map { it.filter }
            asks shouldContain GameObjectFilter.Enchantment.withSubtype(Subtype.AURA.value)
            asks shouldNotContain GameObjectFilter.Artifact.withSubtype("Equipment")
            // A real free equip keeps its read.
            CardIntentAnalyzer.analyze(cardRegistry.getCard("Beatrix, Loyal General")!!).tags shouldContain
                IntentTag.EQUIPMENT_RELOCATION
        }

        test("Spirit Away steals the opponent's creature, not buffs our own") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardInHand(1, "Spirit Away")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Spirit Away")
                shouldTarget("Hill Giant")
            }
        }
    }
}
