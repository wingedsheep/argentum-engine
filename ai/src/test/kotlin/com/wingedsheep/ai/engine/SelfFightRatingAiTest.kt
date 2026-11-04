package com.wingedsheep.ai.engine

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class SelfFightRatingAiTest : FunSpec({
    fun fighter(name: String, strength: Int, deadly: Boolean = false) = card(name) {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = strength
        toughness = 5 - strength
        if (deadly) keywords(Keyword.DEATHTOUCH)
        triggeredAbility {
            trigger = Triggers.self.enters()
            val victim = target(TargetFilter.OtherCreature)
            optional = true
            effect = Effects.Fight(EffectTarget.Self, victim)
        }
    }
    fun CardDefinition.summary() = CardSummary(
        name = name, manaCost = manaCost.toString(), typeLine = typeLine.toString(),
        rarity = metadata.rarity.name, oracleText = oracleText, setCode = "ZZZ",
    )
    val weak = fighter("One power fight rating probe", 1)
    val strong = fighter("Three power fight rating probe", 3)

    test("drafts the stronger self-fight with the same mana cost and total stats") {
        LimitedCardRater.explain(weak).heuristicRating shouldBeLessThan
            LimitedCardRater.explain(strong).heuristicRating
        val registry = CardRegistry().apply { register(listOf(weak, strong)) }
        val bot = EngineAiPlayerController(registry, EntityId.of("bot"), { null })
        bot.chooseDraftPick(listOf(weak.summary(), strong.summary()), emptyList(), 1, 1)
            .shouldContainExactly(listOf(strong.name))
    }

    test("deathtouch preserves full fight reach for a one-power creature") {
        val weakDeadly = fighter("Deadly one power fight rating probe", 1, true)
        val strongDeadly = fighter("Deadly three power fight rating probe", 3, true)
        LimitedCardRater.explain(weakDeadly).heuristicRating shouldBe
            LimitedCardRater.explain(strongDeadly).heuristicRating
    }

    test("variable power and entry counters retain the generic fight estimate") {
        val variable = card("Variable power fight rating probe") {
            manaCost = "{2}{G}"
            typeLine = "Creature — Beast"
            dynamicPower(DynamicAmounts.creaturesYouControl())
            toughness = 4
            triggeredAbility {
                trigger = Triggers.self.enters()
                val victim = target(TargetFilter.OtherCreature)
                effect = Effects.Fight(EffectTarget.Self, victim)
            }
        }
        val growing = card("Entry counter fight rating probe") {
            manaCost = "{2}{G}"
            typeLine = "Creature — Beast"
            power = 0
            toughness = 1
            triggeredAbility {
                trigger = Triggers.self.enters()
                val victim = target(TargetFilter.OtherCreature)
                effect = Effects.Composite(listOf(
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self),
                    Effects.Fight(EffectTarget.Self, victim),
                ))
            }
        }
        variable.creatureStats!!.basePower shouldBe null
        // A generic fight scores 1.0, plus the entry counters' existing 0.4 term.
        LimitedCardRater.explain(variable).heuristic.single { it.part == "Triggered ability 1" }.value shouldBe (0.8 plusOrMinus 0.0001)
        LimitedCardRater.explain(growing).heuristic.single { it.part == "Triggered ability 1" }.value shouldBe (1.12 plusOrMinus 0.0001)
    }

    test("a zero-power creature receives no fight payoff even with deathtouch") {
        val zero = fighter("Zero power fight rating probe", 0, true)
        LimitedCardRater.explain(zero).heuristicRating shouldBeLessThan
            LimitedCardRater.explain(fighter("Positive deadly fight rating probe", 1, true)).heuristicRating
    }
})
