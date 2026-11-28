package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Readings the Dark Ascension cards asked for:
 *
 * - a pipeline that destroys its chosen targets and returns them under *your* control (Sorin, Lord
 *   of Innistrad's −6) is a steal, not a blink: HARMFUL on the target slot, and removal — while a
 *   return under the owner's control (Phelia-style blinks) stays a blink;
 * - attaching a source whose back face takes control of its host, after transforming it (Soul
 *   Seizer into Ghastly Haunting), steals the target: HARMFUL, and removal;
 * - an Equipment unattaching itself before it transforms (Elbrus, the Binding Blade) needs no intent.
 */
class StealAndUnattachReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun unread(name: String) =
        AiReadingAudit.read(card(name)).abilities.flatMap { it.findings }.filterNot { it.read }

    init {
        test("Sorin, Lord of Innistrad's destroy-and-reanimate under our control reads as removal") {
            val sorin = card("Sorin, Lord of Innistrad")
            val ultimate = sorin.script.activatedAbilities.last()
            TargetPolarityAnalyzer.forAbility(sorin, ultimate).toSet() shouldBe setOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(ultimate.effect, ultimate.targetRequirements).tags shouldContain
                IntentTag.REMOVAL
            unread("Sorin, Lord of Innistrad").shouldBeEmpty()
            withClue("an exile its delayed trigger returns stays a blink") {
                val phelia = card("Phelia, Exuberant Shepherd").script.triggeredAbilities.single()
                TargetPolarityAnalyzer.forRequirements(phelia.effect, listOfNotNull(phelia.targetRequirement)) shouldNotBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Soul Seizer's transform-and-attach onto the damaged player's creature reads as a steal") {
            val seizer = card("Soul Seizer")
            val trigger = seizer.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(
                trigger.effect, listOfNotNull(trigger.targetRequirement), source = seizer,
            ) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(
                trigger.effect, listOfNotNull(trigger.targetRequirement), source = seizer,
            ).tags shouldContain IntentTag.REMOVAL
            CardIntentAnalyzer.analyze(seizer).tags shouldContain IntentTag.REMOVAL
            unread("Soul Seizer").shouldBeEmpty()
        }

        test("Elbrus, the Binding Blade unattaching itself before it transforms needs no intent") {
            unread("Elbrus, the Binding Blade").shouldBeEmpty()
        }
    }
}
