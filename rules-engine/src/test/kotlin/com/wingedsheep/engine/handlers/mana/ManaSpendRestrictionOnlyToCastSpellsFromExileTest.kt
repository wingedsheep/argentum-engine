package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test: restricted mana annotated with [ManaRestriction.CastFromExileOnly] must be accepted
 * when paying for a spell cast from exile, and rejected when paying for a spell cast from hand.
 *
 * GIVEN  A player's mana pool contains two mana annotated with CastFromExileOnly
 * AND    The player has one castable spell in hand and one castable spell in exile (with permission)
 * AND    Both spells have a mana cost payable by the two restricted mana
 * WHEN   The engine validates paying each spell's cost using only the restricted mana
 * THEN   Paying the hand spell's cost is rejected (illegal payment)
 * AND    Paying the exile spell's cost is accepted (legal payment)
 */
class ManaSpendRestrictionOnlyToCastSpellsFromExileTest : FunSpec({

    test("restricted mana annotated CastFromExileOnly pays for exile-cast spell but not hand-cast spell") {
        val cost = ManaCost.parse("{R}{R}")
        val pool = ManaPool()
            .addRestricted(Color.RED, 1, ManaRestriction.CastFromExileOnly)
            .addRestricted(Color.RED, 1, ManaRestriction.CastFromExileOnly)

        val handContext = SpellPaymentContext(isFromExile = false)
        val exileContext = SpellPaymentContext(isFromExile = true)

        pool.canPay(cost, handContext) shouldBe false
        pool.canPay(cost, exileContext) shouldBe true
    }
})
