package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AddDynamicManaEffect
import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.OnFirstMainPhase
import com.wingedsheep.rulesengine.ability.OnTransform
import com.wingedsheep.rulesengine.ability.TransformEffect
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.core.Supertype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BrigidClachansHeartTest : FunSpec({

    context("Brigid, Clachan's Heart Front Face Definition") {
        val definition = LorwynEclipsedSet.getCardDefinition("Brigid, Clachan's Heart")!!

        test("has correct name") {
            definition.name shouldBe "Brigid, Clachan's Heart"
        }

        test("has correct mana cost") {
            definition.manaCost shouldBe ManaCost.parse("{2}{W}")
        }

        test("is a creature") {
            definition.isCreature shouldBe true
        }

        test("is legendary") {
            definition.typeLine.supertypes shouldContain Supertype.LEGENDARY
        }

        test("has correct subtypes") {
            definition.typeLine.subtypes shouldContainAll setOf(Subtype.KITHKIN, Subtype.WARRIOR)
        }

        test("has correct power and toughness") {
            definition.creatureStats?.basePower shouldBe 3
            definition.creatureStats?.baseToughness shouldBe 2
        }

        test("is a double-faced card") {
            definition.isDoubleFaced shouldBe true
            definition.backFace shouldNotBe null
        }
    }

    context("Brigid Back Face Definition") {
        val definition = LorwynEclipsedSet.getCardDefinition("Brigid, Clachan's Heart")!!
        val backFace = definition.backFace!!

        test("has correct back face name") {
            backFace.name shouldBe "Brigid, Doun's Mind"
        }

        test("back face has no mana cost") {
            backFace.manaCost shouldBe ManaCost.ZERO
        }

        test("back face is legendary") {
            backFace.typeLine.supertypes shouldContain Supertype.LEGENDARY
        }

        test("back face has correct subtypes") {
            backFace.typeLine.subtypes shouldContainAll setOf(Subtype.KITHKIN, Subtype.SOLDIER)
        }

        test("back face has correct power and toughness") {
            backFace.creatureStats?.basePower shouldBe 3
            backFace.creatureStats?.baseToughness shouldBe 2
        }
    }

    context("Brigid Front Face Script") {
        val script = LorwynEclipsedSet.getCardScript("Brigid, Clachan's Heart")!!

        test("has three triggered abilities") {
            script.triggeredAbilities shouldHaveSize 3
        }

        test("has ETB trigger that creates token") {
            val etbTrigger = script.triggeredAbilities.find { it.trigger is OnEnterBattlefield }
            etbTrigger shouldNotBe null
            etbTrigger!!.effect.shouldBeInstanceOf<CreateTokenEffect>()
            val tokenEffect = etbTrigger.effect as CreateTokenEffect
            tokenEffect.count shouldBe 1
            tokenEffect.power shouldBe 1
            tokenEffect.toughness shouldBe 1
            tokenEffect.colors shouldContainAll setOf(Color.GREEN, Color.WHITE)
            tokenEffect.creatureTypes shouldContain "Kithkin"
        }

        test("has transform trigger when transforming to front") {
            val transformTrigger = script.triggeredAbilities.find { it.trigger is OnTransform }
            transformTrigger shouldNotBe null
            val trigger = transformTrigger!!.trigger as OnTransform
            trigger.selfOnly shouldBe true
            trigger.intoBackFace shouldBe false  // Triggers when transforming INTO front face
        }

        test("has first main phase trigger for transform") {
            val mainPhaseTrigger = script.triggeredAbilities.find { it.trigger is OnFirstMainPhase }
            mainPhaseTrigger shouldNotBe null
            mainPhaseTrigger!!.optional shouldBe true
            mainPhaseTrigger.effect.shouldBeInstanceOf<TransformEffect>()
        }
    }

    context("Brigid Back Face Script") {
        val script = LorwynEclipsedSet.getCardScript("Brigid, Doun's Mind")!!

        test("has activated mana ability") {
            script.activatedAbilities shouldHaveSize 1
            val ability = script.activatedAbilities.first()
            ability.isManaAbility shouldBe true
        }

        test("mana ability costs tap") {
            val ability = script.activatedAbilities.first()
            ability.cost shouldBe AbilityCost.Tap
        }

        test("mana ability produces dynamic mana") {
            val ability = script.activatedAbilities.first()
            ability.effect.shouldBeInstanceOf<AddDynamicManaEffect>()
            val effect = ability.effect as AddDynamicManaEffect
            effect.amountSource shouldBe DynamicAmount.OtherCreaturesYouControl
            effect.allowedColors shouldContainAll setOf(Color.GREEN, Color.WHITE)
        }

        test("has first main phase trigger for transform") {
            script.triggeredAbilities shouldHaveSize 1
            val trigger = script.triggeredAbilities.first()
            trigger.trigger.shouldBeInstanceOf<OnFirstMainPhase>()
            trigger.optional shouldBe true
            trigger.effect.shouldBeInstanceOf<TransformEffect>()
        }
    }
})
