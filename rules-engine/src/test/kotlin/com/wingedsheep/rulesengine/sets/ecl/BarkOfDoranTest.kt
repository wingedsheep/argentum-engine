package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.ability.AssignDamageEqualToToughness
import com.wingedsheep.rulesengine.ability.ModifyStats
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.core.ManaCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BarkOfDoranTest : FunSpec({

    context("Bark of Doran Definition") {
        val definition = LorwynEclipsedSet.getCardDefinition("Bark of Doran")!!

        test("has correct name") {
            definition.name shouldBe "Bark of Doran"
        }

        test("has correct mana cost") {
            definition.manaCost shouldBe ManaCost.parse("{1}{W}")
        }

        test("is an equipment") {
            definition.isEquipment shouldBe true
        }

        test("has correct equip cost") {
            definition.equipCost shouldBe ManaCost.parse("{1}")
        }

        test("has oracle text describing abilities") {
            definition.oracleText.contains("+0/+1") shouldBe true
            definition.oracleText.contains("toughness is greater than its power") shouldBe true
        }
    }

    context("Bark of Doran Script") {
        val script = LorwynEclipsedSet.getCardScript("Bark of Doran")!!

        test("has two static abilities") {
            script.staticAbilities shouldHaveSize 2
        }

        test("has ModifyStats ability for +0/+1") {
            val modifyStats = script.staticAbilities.filterIsInstance<ModifyStats>().first()
            modifyStats.powerBonus shouldBe 0
            modifyStats.toughnessBonus shouldBe 1
            modifyStats.target shouldBe StaticTarget.AttachedCreature
        }

        test("has AssignDamageEqualToToughness ability") {
            val damageAbility = script.staticAbilities.filterIsInstance<AssignDamageEqualToToughness>().first()
            damageAbility.target shouldBe StaticTarget.AttachedCreature
            damageAbility.onlyWhenToughnessGreaterThanPower shouldBe true
        }
    }
})
