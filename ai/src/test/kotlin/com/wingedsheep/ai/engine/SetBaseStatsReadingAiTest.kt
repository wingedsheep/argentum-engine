package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A base-P/T change on a target ([com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect]).
 *
 * "Target creature has base power and toughness 0/0 until end of turn" (Multiply by Zero) is
 * removal: it kills anything not propped up by counters or a pump. Unread, the card had no intent
 * at all, so nothing that holds removal for a response or the end step saw it as removal. A base of
 * 1/1 or smaller, or one with no power left (Sorceress Queen's 0/2), is the curse side of the table.
 * A larger fixed base grows a small creature and shrinks a big one: alone it reads flexible (both
 * sides stay candidates for the simulation), and beside an effect that reads the slot one way, that
 * effect decides.
 */
class SetBaseStatsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a base 0/0 on a target creature is removal aimed at their side") {
            val card = cardRegistry.getCard("Multiply by Zero")!!
            withClue("its slot reads as harmful") {
                TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("and the card is removal") {
                intents.forName("Multiply by Zero")!!.tags shouldContain IntentTag.REMOVAL
            }
        }

        test("a base with no power left is a curse") {
            withClue("Sorceress Queen: target creature other than this has base power and toughness 0/2") {
                val ability = cardRegistry.getCard("Sorceress Queen")!!.script.activatedAbilities.single()
                TargetPolarityAnalyzer.forRequirements(ability.effect, ability.targetRequirements) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("a larger fixed base on its own reads flexible") {
            withClue("Square Up: target creature has base power and toughness 4/4") {
                TargetPolarityAnalyzer.forSpell(cardRegistry.getCard("Square Up")!!) shouldBe
                    listOf(TargetPolarity.FLEXIBLE)
            }
            withClue("Perfected Theory: one mode shrinks to 1/1, the other sets 4/5") {
                val modes = (cardRegistry.getCard("Perfected Theory")!!.script.spellEffect
                    as com.wingedsheep.sdk.scripting.effects.ModalEffect).modes
                modes.map { TargetPolarityAnalyzer.forMode(it) } shouldBe
                    listOf(listOf(TargetPolarity.HARMFUL), listOf(TargetPolarity.FLEXIBLE))
            }
        }

        test("a larger base leaves the slot to the effects beside it") {
            // Taranika: untap target creature you control; it has base 4/4 and gains
            // indestructible — the untap and the keyword read the slot, the 4/4 does not veto them.
            val trigger = cardRegistry.getCard("Taranika, Akroan Veteran")!!.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }
    }
}
