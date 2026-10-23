package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.mechanics.targeting.withTargetConstraints
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `TargetObject.differentNumericProperty` — "targets with different mana values" (Long Rest): one
 * exclusive pool per value at announcement, the authoritative cast check, the interactive decision
 * check, and the set-wide re-check at resolution.
 */
class DifferentNumericPropertyTargetingTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40))
    }
    val anyNumber = TargetObject(filter = TargetFilter.Creature, unlimited = true,
        differentNumericProperty = CardNumericProperty.MANA_VALUE)
    val enumeration = TargetEnumerationUtils(PredicateEvaluator(cardRegistry = null))

    test("announcement offers one exclusive pool per value and caps the count at the distinct values") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val info = enumeration.buildTargetInfos(d.state, d.player1, listOf(anyNumber)).single()
        info.exclusiveGroups!!.map { it.toSet() }.toSet() shouldBe setOf(setOf(a, b), setOf(giant))
        info.maxTargets shouldBe 2
        info.validTargets.toSet() shouldBe setOf(a, b, giant)
    }

    test("a mandatory count larger than the distinct values leaves no legal choice") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val three = TargetObject(filter = TargetFilter.Creature, count = 3,
            differentNumericProperty = CardNumericProperty.MANA_VALUE)
        enumeration.buildTargetInfos(d.state, d.player1, listOf(three)).single().validTargets shouldBe emptyList()
    }

    test("the cast validator rejects two targets sharing a value") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val validator = TargetValidator(PredicateEvaluator(cardRegistry = null))
        fun validate(ids: List<com.wingedsheep.sdk.model.EntityId>) = validator.validateTargets(d.state,
            ids.map { ChosenTarget.Permanent(it) }, listOf(anyNumber), d.player1, targetingSourceType = TargetingSourceType.SPELL)
        validate(listOf(a, b)).shouldNotBeNull()
        validate(listOf(a, giant)).shouldBeNull()
    }

    test("an interactive target decision holds the selection to the exclusive pools") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val candidates = listOf(a, b, giant)
        val info = TargetRequirementInfo(0, anyNumber.description, 0, candidates.size)
            .withTargetConstraints(d.state, anyNumber, candidates)
        info.maxTargets shouldBe 2
        val decision = ChooseTargetsDecision("different", d.player1, "Choose", DecisionContext(),
            listOf(info), mapOf(0 to candidates))
        DecisionValidators.validate(decision, TargetsResponse("different", mapOf(0 to listOf(a, b))), d.state).shouldNotBeNull()
        DecisionValidators.validate(decision, TargetsResponse("different", mapOf(0 to listOf(a, giant))), d.state).shouldBeNull()
    }

    test("targets that come to share a value before resolution are all illegal") {
        val d = setup()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val spell = card("Different Power Test") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                targets(TargetFilter.Creature, count = 2, differentNumericProperty = CardNumericProperty.POWER)
                effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
            }
        }
        d.registerCards(listOf(spell))
        val p = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(p, "Hill Giant")
        d.giveMana(p, Color.GREEN, 1)
        d.castSpellWithTargets(p, d.putCardInHand(p, spell.name),
            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))).error shouldBe null
        // Grizzly Bears grows to 3 power — now equal to Hill Giant's.
        d.addComponent(bears, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        d.bothPass()
        (bears in d.state.getBattlefield()) shouldBe true
        (giant in d.state.getBattlefield()) shouldBe true
    }
})
