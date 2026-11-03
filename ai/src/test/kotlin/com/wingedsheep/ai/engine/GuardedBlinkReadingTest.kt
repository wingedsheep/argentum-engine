package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

class GuardedBlinkReadingTest : FunSpec({
    fun guarded(name: String, filter: GameObjectFilter, otherTarget: Boolean = false) = card(name) {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            val guardTarget = if (otherTarget) EffectTarget.Self else creature
            effect = Effects.Move(creature, Zone.EXILE) then Effects.If(
                Conditions.TargetMatchesFilter(filter, guardTarget),
                then = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.EXILE) then Effects.DrawCards(1),
            )
        }
    }

    test("the exact successful-exile guard preserves blink protection and the draw rider") {
        val card = guarded("Exact guarded blink", GameObjectFilter.Any.currentlyIn(Zone.EXILE))
        val effect = card.script.spellEffect!!
        EffectWalker.blinkMoves(effect).size shouldBe 2
        CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.DRAW
        CardIntentAnalyzer.analyze(card).tags shouldNotContain IntentTag.EXILE_REMOVAL
        TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.BENEFICIAL)
        LimitedCardRater.effectValue(effect) shouldBeLessThan 1.0
    }

    test("a characteristic condition does not turn conditional exile removal into protection") {
        val card = guarded("Characteristic gated return", GameObjectFilter.Creature)
        EffectWalker.blinkMoves(card.script.spellEffect!!) shouldBe emptyList()
        CardIntentAnalyzer.analyze(card).tags shouldNotContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.EXILE_REMOVAL
    }

    test("an exile check on another object does not establish that this target returns") {
        val card = guarded("Other object gated return", GameObjectFilter.Any.currentlyIn(Zone.EXILE), true)
        EffectWalker.blinkMoves(card.script.spellEffect!!) shouldBe emptyList()
        CardIntentAnalyzer.analyze(card).tags shouldNotContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.EXILE_REMOVAL
    }
})
