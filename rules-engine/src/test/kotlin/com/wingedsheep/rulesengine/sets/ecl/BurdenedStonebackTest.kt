package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BurdenedStonebackTest : FunSpec({

    context("Burdened Stoneback Definition") {
        val definition = LorwynEclipsedSet.getCardDefinition("Burdened Stoneback")!!

        test("has correct name") {
            definition.name shouldBe "Burdened Stoneback"
        }

        test("has correct mana cost") {
            definition.manaCost shouldBe ManaCost.parse("{1}{W}")
        }

        test("is a creature") {
            definition.isCreature shouldBe true
        }

        test("has correct subtypes") {
            definition.typeLine.subtypes shouldContainAll setOf(Subtype.GIANT, Subtype.WARRIOR)
        }

        test("has correct power and toughness") {
            definition.creatureStats?.basePower shouldBe 4
            definition.creatureStats?.baseToughness shouldBe 4
        }
    }

    context("Burdened Stoneback Script") {
        val script = LorwynEclipsedSet.getCardScript("Burdened Stoneback")!!

        test("has ETB triggered ability") {
            script.triggeredAbilities shouldHaveSize 1
            val trigger = script.triggeredAbilities.first().trigger
            trigger.shouldBeInstanceOf<OnEnterBattlefield>()
        }

        test("ETB adds two -1/-1 counters") {
            val effect = script.triggeredAbilities.first().effect
            effect.shouldBeInstanceOf<AddCountersEffect>()
            val addCounters = effect as AddCountersEffect
            addCounters.counterType shouldBe "-1/-1"
            addCounters.count shouldBe 2
            addCounters.target shouldBe EffectTarget.Self
        }

        test("has activated ability") {
            script.activatedAbilities shouldHaveSize 1
        }

        test("activated ability has correct cost") {
            val ability = script.activatedAbilities.first()
            val cost = ability.cost
            cost.shouldBeInstanceOf<AbilityCost.Composite>()
            val composite = cost as AbilityCost.Composite
            composite.costs shouldHaveSize 2

            // Mana cost: {1}{W}
            val manaCost = composite.costs.filterIsInstance<AbilityCost.Mana>().first()
            manaCost.white shouldBe 1
            manaCost.generic shouldBe 1

            // Remove counter cost
            val removeCounter = composite.costs.filterIsInstance<AbilityCost.RemoveCounter>().first()
            removeCounter.counterType shouldBe "any"
            removeCounter.count shouldBe 1
        }

        test("activated ability grants indestructible") {
            val ability = script.activatedAbilities.first()
            val effect = ability.effect
            effect.shouldBeInstanceOf<GrantKeywordUntilEndOfTurnEffect>()
            val grantEffect = effect as GrantKeywordUntilEndOfTurnEffect
            grantEffect.keyword shouldBe Keyword.INDESTRUCTIBLE
            grantEffect.target shouldBe EffectTarget.TargetCreature
        }

        test("activated ability has sorcery timing restriction") {
            val ability = script.activatedAbilities.first()
            ability.timingRestriction shouldBe TimingRestriction.SORCERY
        }
    }
})
