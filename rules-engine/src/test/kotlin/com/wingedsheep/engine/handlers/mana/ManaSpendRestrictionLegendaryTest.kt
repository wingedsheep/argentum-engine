package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [ManaRestriction.LegendarySpellsOnly] (Great Hall of the Citadel, Delighted Halfling):
 * "Spend this mana only to cast legendary spells." Mirrors the card-type restriction tests.
 */
class ManaSpendRestrictionLegendaryTest : FunSpec({

    test("legendary-spells-only mana pays for a legendary spell but not a non-legendary one") {
        val cost = ManaCost.parse("{W}")
        val pool = ManaPool().addRestricted(Color.WHITE, 1, ManaRestriction.LegendarySpellsOnly)

        val legendarySpell = SpellPaymentContext(isLegendary = true)
        val plainSpell = SpellPaymentContext(isLegendary = false)

        pool.canPay(cost, legendarySpell) shouldBe true
        pool.canPay(cost, plainSpell) shouldBe false
    }

    test("legendary-spells-only mana is rejected for ability activations") {
        val cost = ManaCost.parse("{W}")
        val pool = ManaPool().addRestricted(Color.WHITE, 1, ManaRestriction.LegendarySpellsOnly)

        // Even if the source is legendary, an ability activation is not casting a legendary spell.
        val abilityActivation = SpellPaymentContext(isAbilityActivation = true, isLegendary = true)

        pool.canPay(cost, abilityActivation) shouldBe false
    }
})
