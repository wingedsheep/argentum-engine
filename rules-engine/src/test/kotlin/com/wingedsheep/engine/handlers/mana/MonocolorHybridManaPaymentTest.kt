package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Monocolored hybrid ("twobrid") mana payment — a {2/B} pip is payable with the generic
 * amount OR one mana of the color, whichever the pool can afford. Drives Gurmag Nightwatch
 * ({2/B}{2/G}{2/U}). See [com.wingedsheep.sdk.core.MonocolorHybridManaTest] for parsing/CMC.
 */
class MonocolorHybridManaPaymentTest : StringSpec({

    val cost2B = ManaCost.parse("{2/B}")
    val gurmag = ManaCost.parse("{2/B}{2/G}{2/U}")

    "{2/B} is payable with one black mana" {
        val pool = ManaPool(black = 1)
        pool.canPay(cost2B) shouldBe true
        pool.pay(cost2B) shouldBe ManaPool()
    }

    "{2/B} is payable with two generic mana of any type" {
        val pool = ManaPool(colorless = 2)
        pool.canPay(cost2B) shouldBe true
        pool.pay(cost2B) shouldBe ManaPool()
    }

    "{2/B} is NOT payable with a single colorless mana" {
        val pool = ManaPool(colorless = 1)
        pool.canPay(cost2B) shouldBe false
        pool.pay(cost2B) shouldBe null
    }

    "{2/B} prefers the cheaper colored side, leaving other mana in the pool" {
        // Both a black mana and a colorless are present; paying the {B} side spends one mana
        // (the black) rather than two, leaving the colorless behind.
        val pool = ManaPool(black = 1, colorless = 1)
        pool.pay(cost2B) shouldBe ManaPool(colorless = 1)
    }

    "Gurmag Nightwatch is payable with one mana of each color" {
        val pool = ManaPool(black = 1, green = 1, blue = 1)
        pool.canPay(gurmag) shouldBe true
        pool.pay(gurmag) shouldBe ManaPool()
    }

    "Gurmag Nightwatch is payable with six generic mana" {
        val pool = ManaPool(colorless = 6)
        pool.canPay(gurmag) shouldBe true
        pool.pay(gurmag) shouldBe ManaPool()
    }

    "Gurmag Nightwatch is payable mixing one colored side and generic for the rest" {
        // {B} side paid with black (1), the {2/G} and {2/U} sides paid with 4 generic.
        val pool = ManaPool(black = 1, colorless = 4)
        pool.canPay(gurmag) shouldBe true
        pool.pay(gurmag) shouldBe ManaPool()
    }

    "Gurmag Nightwatch is NOT payable with five generic mana" {
        // Three twobrid pips, no usable color: cheapest is 2+2+2 = 6 generic.
        val pool = ManaPool(colorless = 5)
        pool.canPay(gurmag) shouldBe false
    }

    "a strict colored pip claims its color before a same-color twobrid falls back to generic" {
        // {B}{2/B} with one black + two colorless: {B} takes the black, {2/B} pays 2 generic.
        val pool = ManaPool(black = 1, colorless = 2)
        ManaCost.parse("{B}{2/B}").let {
            pool.canPay(it) shouldBe true
            pool.pay(it) shouldBe ManaPool()
        }
    }
})
