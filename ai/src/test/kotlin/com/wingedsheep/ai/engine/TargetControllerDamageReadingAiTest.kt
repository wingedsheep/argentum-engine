package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Harm aimed at a target's controller — "deals damage to target spell's controller equal to that
 * spell's mana value" (Refuse), "destroy target artifact; Detonate deals X damage to that
 * artifact's controller" — names the target through `EffectTarget.TargetController`, not its
 * slot. Read as harm to the target's side, the AI points Refuse at an opponent's spell instead of
 * treating the slot as two-sided and possibly burning itself with its own spell.
 */
class TargetControllerDamageReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("damage to a target spell's controller reads as harm to that spell's side") {
            val refuse = cardRegistry.getCard("Refuse // Cooperate")!!.cardFaces.first { it.name == "Refuse" }
            intents.effectTargetPolarities(refuse.script.spellEffect!!, refuse.script.targetRequirements) shouldBe
                listOf(TargetPolarity.HARMFUL)
        }

        test("a removal spell that also burns the target's controller stays harmful") {
            withClue("Detonate: destroy target artifact, X damage to its controller") {
                val detonate = cardRegistry.getCard("Detonate")!!.script
                intents.effectTargetPolarities(detonate.spellEffect!!, detonate.targetRequirements) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }
    }
}
