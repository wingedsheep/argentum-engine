package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Target creature an opponent controls attacks … if able"
 * ([com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect]) — this turn (Nettling Imp), or
 * during its controller's next turn, at a named planeswalker (Gideon, Battle-Forged's +2): the
 * single-creature form of a forced attack. Its slot reads [TargetPolarity.HARMFUL] and it is an
 * [IntentTag.FORCED_ATTACK], as is the per-creature group form over the opponents' creatures
 * (Bident of Thassa). A mark on our own fresh token (Legion Warboss) is a price, not a payoff.
 */
class MustAttackMarkReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("a forced attack on a chosen creature reads as harmful to it and as a forced attack") {
            val imp = card("Nettling Imp")
            val ability = imp.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(imp, ability) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements).tags shouldContain IntentTag.FORCED_ATTACK
            for (name in listOf("Nettling Imp", "Bident of Thassa", "Grizzled Angler", "Legion Warboss")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.flags.none { "MarkMustAttackEffect" in it.key } shouldBe true }
            }
        }

        test("the next-turn mark aimed at a planeswalker reads the same") {
            val effect = Effects.MarkMustAttackNextTurn(EffectTarget.ContextTarget(0), EffectTarget.Self)
            CardIntentAnalyzer.analyzeEffect(effect).tags shouldContain IntentTag.FORCED_ATTACK
        }

        test("marking each of the opponents' creatures is a forced attack; marking our own token is not") {
            val bident = card("Bident of Thassa").script.activatedAbilities.single()
            CardIntentAnalyzer.analyzeEffect(bident.effect, bident.targetRequirements).tags shouldContain IntentTag.FORCED_ATTACK

            val ours = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity),
            )
            CardIntentAnalyzer.analyzeEffect(ours).tags shouldNotContain IntentTag.FORCED_ATTACK

            val warboss = card("Legion Warboss")
            warboss.script.triggeredAbilities.forEach { ability ->
                CardIntentAnalyzer.analyzeEffect(ability.effect).tags shouldNotContain IntentTag.FORCED_ATTACK
            }
        }
    }
}
