package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.mtg.sets.definitions.iko.cards.EscapeProtocol
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.xln.cards.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/** Reader-level regressions for compositions, rather than card rules scenarios. */
class CompositeSupportReadingTest : FunSpec({
    test("a blink is protection and its rating does not include exile removal") {
        val intent = CardIntentAnalyzer.analyze(SirensRuse)
        intent.tags shouldContain IntentTag.PROTECTION
        intent.tags shouldContain IntentTag.DRAW
        intent.tags shouldNotContain IntentTag.REMOVAL
        intent.tags shouldNotContain IntentTag.EXILE_REMOVAL
        TargetPolarityAnalyzer.forSpell(SirensRuse) shouldBe listOf(TargetPolarity.BENEFICIAL)
        LimitedCardRater.effectValue(SirensRuse.script.spellEffect!!) shouldBeLessThan 1.0
    }

    test("a reflexive blink retains its composite protection reading") {
        val effect = EscapeProtocol.script.triggeredAbilities.single().effect
        CardIntentAnalyzer.analyze(EscapeProtocol).tags shouldContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyzeEffect(effect).tags shouldContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyzeEffect(effect).tags shouldNotContain IntentTag.EXILE_REMOVAL
        LimitedCardRater.effectValue(effect) shouldBeGreaterThan 0.0
        AiReadingAudit.read(EscapeProtocol).abilities.flatMap { it.findings }
            .filter { !it.read } shouldBe emptyList()
    }

    test("conditional self statics expose their pump and evasion") {
        val intent = CardIntentAnalyzer.analyze(JacesSentinel)
        intent.tags shouldContain IntentTag.PUMP
        intent.tags shouldContain IntentTag.EVASION_GRANT
        intent.tags shouldNotContain IntentTag.ANTHEM
    }

    test("temporary global triggers expose their later payoff") {
        val effect = JaceCunningCastaway.script.activatedAbilities.first().effect
        CardIntentAnalyzer.analyzeEffect(effect).tags shouldContain IntentTag.DRAW
        LimitedCardRater.effectValue(effect) shouldBeGreaterThan 0.0
    }

    test("target iteration preserves either-way bounce while opponent-controlled groups are offensive") {
        TargetPolarityAnalyzer.forSpell(GraspingCurrent) shouldBe listOf(TargetPolarity.FLEXIBLE)
        val ability = TempestCaller.script.triggeredAbilities.single()
        TargetPolarityAnalyzer.forRequirements(ability.effect, listOfNotNull(ability.targetRequirement)) shouldBe
            listOf(TargetPolarity.HARMFUL)
    }

    test("targeted mill has a harmful default and a small heuristic term") {
        for (card in listOf(FleetSwallower, NavigatorsRuin)) {
            val ability = card.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(ability.effect, listOfNotNull(ability.targetRequirement)) shouldBe
                listOf(TargetPolarity.HARMFUL)
            LimitedCardRater.effectValue(ability.effect) shouldBeGreaterThan 0.0
            AiReadingAudit.read(card).abilities.flatMap { it.findings }
                .filter { it.check == AiReadingAudit.Check.POLARITY && !it.read } shouldBe emptyList()
        }
    }

    test("discarding feeds the graveyard without claiming to reward it") {
        for (card in listOf(DaringSaboteur, ShipwreckLooter)) {
            GraveyardSynergy.isPayoff(card) shouldBe false
            SynergyAxes.of(card).feeds shouldContain SynergyAxis.GRAVEYARD
            SynergyAxes.of(card).rewards shouldNotContain SynergyAxis.GRAVEYARD
        }
        GraveyardSynergy.isPayoff(JaceCunningCastaway) shouldBe false
        SynergyAxes.of(JaceCunningCastaway).feeds shouldContain SynergyAxis.GRAVEYARD
        SynergyAxes.of(JaceCunningCastaway).rewards shouldNotContain SynergyAxis.GRAVEYARD
        // The Illusion is still a creature to sacrifice; Jace copies alone are not creatures.
        SynergyAxes.of(JaceCunningCastaway).feeds shouldContain SynergyAxis.DEATH
        GraveyardSynergy.isPayoff(SearchForAzcanta) shouldBe true
    }
})
