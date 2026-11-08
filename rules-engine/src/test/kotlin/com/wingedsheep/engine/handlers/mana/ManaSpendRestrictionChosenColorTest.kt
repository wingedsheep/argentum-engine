package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.SpellsWithExactColorCount]'s `includingColor` axis and its unbound chosen-color
 * form [ManaRestriction.SpellsOfChosenColor] — Throne of Eldraine's "Spend this mana only to cast
 * monocolored spells of that color."
 */
class ManaSpendRestrictionChosenColorTest : FunSpec({

    fun spell(vararg colors: Color) = SpellPaymentContext(
        cardTypes = setOf(CardType.CREATURE),
        colorCount = colors.size,
        colors = colors.toSet(),
        isColorless = colors.isEmpty(),
    )

    val monoBlue = ManaRestriction.SpellsWithExactColorCount(1, Color.BLUE)
    val pool = ManaPool().addRestricted(Color.BLUE, 4, monoBlue)
    val cost = ManaCost.parse("{1}")

    test("a monocolored spell of the color qualifies") {
        pool.canPay(cost, spell(Color.BLUE)) shouldBe true
    }

    test("a monocolored spell of another color does not") {
        pool.canPay(cost, spell(Color.RED)) shouldBe false
    }

    test("a multicolored spell that includes the color does not — monocolored means exactly one color") {
        pool.canPay(cost, spell(Color.BLUE, Color.RED)) shouldBe false
        pool.canPay(cost, spell(Color.BLUE, Color.RED, Color.GREEN)) shouldBe false
    }

    test("a colorless spell does not") {
        pool.canPay(cost, spell()) shouldBe false
        pool.canPay(cost, SpellPaymentContext.faceDownCast()) shouldBe false
    }

    test("a context that doesn't know the spell's colors refuses the mana") {
        // colorCount alone, with no colors, can't establish that the spell is *blue*.
        pool.canPay(cost, SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), colorCount = 1)) shouldBe false
        pool.canPay(cost, SpellPaymentContext(cardTypes = setOf(CardType.CREATURE))) shouldBe false
    }

    test("ability activations and special actions never qualify") {
        pool.canPay(cost, SpellPaymentContext(isAbilityActivation = true, colorCount = 1, colors = setOf(Color.BLUE))) shouldBe false
        pool.canPay(cost, SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe false
        pool.canPay(cost, null) shouldBe false
    }

    test("the restricted mana pays any part of the spell's cost, including generic") {
        // {2}{R}-style mix: the restriction gates the spell, not the symbol — a blue spell's {2} and {U}.
        val blue = ManaCost.parse("{3}{U}")
        pool.canPay(blue, spell(Color.BLUE)) shouldBe true
        pool.pay(blue, spell(Color.BLUE))!!.restrictedMana.size shouldBe 0
    }

    test("an unconstrained color count is unchanged by the new axis") {
        val meeting = ManaRestriction.SpellsWithExactColorCount(3)
        meeting.includingColor shouldBe null
        meeting.isSatisfiedBy(spell(Color.WHITE, Color.BLUE, Color.BLACK)) shouldBe true
        meeting.isSatisfiedBy(spell(Color.WHITE, Color.BLUE)) shouldBe false
        meeting.description shouldBe "Spend this mana only to cast spells with exactly three colors"
    }

    test("a multicolored count with a color needs both the count and the color") {
        val twoColorsWithRed = ManaRestriction.SpellsWithExactColorCount(2, Color.RED)
        twoColorsWithRed.isSatisfiedBy(spell(Color.RED, Color.GREEN)) shouldBe true
        twoColorsWithRed.isSatisfiedBy(spell(Color.BLUE, Color.GREEN)) shouldBe false
        twoColorsWithRed.isSatisfiedBy(spell(Color.RED)) shouldBe false
    }

    test("the unbound chosen-color form pays for nothing") {
        ManaRestriction.SpellsOfChosenColor(1).isSatisfiedBy(spell(Color.BLUE)) shouldBe false
    }

    test("descriptions read the way Throne of Eldraine prints them") {
        ManaRestriction.SpellsOfChosenColor(1).description shouldBe
            "Spend this mana only to cast monocolored spells of the chosen color"
        monoBlue.description shouldBe "Spend this mana only to cast monocolored blue spells"
    }
})
