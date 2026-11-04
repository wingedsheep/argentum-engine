package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.xln.cards.*
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.core.TypeLine
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/** Structural cost and combat-drawback readings, independent of card rules. */
class ActivatedPayoffReadingTest : FunSpec({
    test("eight mana sacrifice payoff keeps half the value of the same affordable payoff") {
        val keeper = FireShrineKeeper
        val cheap = keeper.copy(name = "Affordable sacrifice payoff", script = keeper.script.copy(
            activatedAbilities = keeper.script.activatedAbilities.map {
                it.copy(cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap, Costs.SacrificeSelf))
            }
        ))
        fun abilityValue(card: com.wingedsheep.sdk.model.CardDefinition) = LimitedCardRater.explain(card)
            .heuristic.single { it.part == "Activated ability 1" }.value
        abilityValue(keeper) shouldBe abilityValue(cheap) * 0.5
        LimitedCardRater.rate(keeper) shouldBeLessThan LimitedCardRater.rate(cheap)
        SynergyAxes.of(keeper).feeds shouldContain SynergyAxis.DEATH
        SynergyAxes.of(keeper).rewards shouldNotContain SynergyAxis.DEATH
        val artifact = keeper.copy(name = "Artifact sacrifice payoff", typeLine = TypeLine.parse("Artifact"))
        SynergyAxes.of(artifact).feeds shouldNotContain SynergyAxis.DEATH
    }

    test("self combat restrictions are read as drawbacks and discounted") {
        for (card in listOf(BondedHorncrest, HeadstrongBrute)) {
            val intent = CardIntentAnalyzer.analyze(card)
            intent.tags shouldContain IntentTag.COMBAT_RESTRICTION
            intent.tags shouldNotContain IntentTag.NEUTRALIZE
            val drawbacks = LimitedCardRater.explain(card).heuristic.filter {
                it.part.startsWith("Static ability") && it.value < 0
            }
            drawbacks.sumOf { it.value } shouldBeLessThan 0.0
        }
        LimitedCardRater.explain(BondedHorncrest).heuristic.filter {
            it.part.startsWith("Static ability")
        }.sumOf { it.value } shouldBe -0.5
    }

    test("repeatable theft retains board value but entry damage and sacrifice damage do not") {
        CardIntentAnalyzer.analyzeInPlay(CaptivatingCrew).staticPriorValue shouldBeGreaterThan 0.5
        CardIntentAnalyzer.analyzeInPlay(BurningSunsAvatar).staticPriorValue shouldBe 0.5
        CardIntentAnalyzer.analyzeInPlay(FireShrineKeeper).staticPriorValue shouldBe 0.5
    }
})
