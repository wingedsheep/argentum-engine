package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [ManaRestriction.SpellsWithExactColorCount] (Meeting of the Five): "Spend this mana
 * only to cast spells with exactly three colors."
 */
class ManaSpendRestrictionColorCountTest : FunSpec({

    val cost = ManaCost.parse("{1}")
    val pool = ManaPool().addRestricted(Color.GREEN, 1, ManaRestriction.SpellsWithExactColorCount(3))
    fun spell(colorCount: Int?) =
        SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), colorCount = colorCount)

    test("pays for a spell with exactly three colors, not two or four") {
        pool.canPay(cost, spell(3)) shouldBe true
        pool.canPay(cost, spell(2)) shouldBe false
        pool.canPay(cost, spell(4)) shouldBe false
    }

    test("a context that doesn't know the spell's colors refuses the mana") {
        pool.canPay(cost, spell(null)) shouldBe false
    }

    test("is rejected for ability activations") {
        pool.canPay(cost, SpellPaymentContext(isAbilityActivation = true, colorCount = 3)) shouldBe false
    }

    test("a face-down spell is colorless") {
        pool.canPay(cost, SpellPaymentContext.faceDownCast()) shouldBe false
        SpellPaymentContext.faceDownCast().colorCount shouldBe 0
    }
})
