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
 * Restricted mana keyed to a spell's graveyard origin ([ManaRestriction.CastFromGraveyardOnly],
 * Rootcoil Creeper) and to a disturb cast ([ManaRestriction.DisturbCostOnly], Unblinking
 * Observer). Each is accepted only for its own spend context: graveyard mana pays any cast from
 * the caster's graveyard (flashback, disturb) but not a hand cast; disturb mana pays a disturb
 * cast but neither a hand cast nor a flashback cast; neither pays an ability activation.
 */
class ManaSpendRestrictionGraveyardAndDisturbTest : FunSpec({

    val cost = ManaCost.parse("{U}{U}")

    fun poolOf(restriction: ManaRestriction) = ManaPool()
        .addRestricted(Color.BLUE, 1, restriction)
        .addRestricted(Color.BLUE, 1, restriction)

    val handCast = SpellPaymentContext(cardTypes = setOf(CardType.INSTANT), isInstantOrSorcery = true)
    val flashbackCast = handCast.copy(isFromHand = false, isFromGraveyard = true)
    val disturbCast = SpellPaymentContext(
        cardTypes = setOf(CardType.CREATURE), isCreature = true,
        isFromHand = false, isFromGraveyard = true, isDisturbCast = true,
    )
    val abilityActivation = SpellPaymentContext(
        isAbilityActivation = true, abilitySourceCardTypes = setOf(CardType.CREATURE),
    )

    test("graveyard-only mana pays a flashback cast and a disturb cast, not a hand cast") {
        val pool = poolOf(ManaRestriction.CastFromGraveyardOnly)
        pool.canPay(cost, flashbackCast) shouldBe true
        pool.canPay(cost, disturbCast) shouldBe true
        pool.canPay(cost, handCast) shouldBe false
    }

    test("graveyard-only mana can't pay a cast from an opponent's graveyard or exile") {
        val pool = poolOf(ManaRestriction.CastFromGraveyardOnly)
        pool.canPay(cost, handCast.copy(isFromHand = false, isFromGraveyard = false)) shouldBe false
        pool.canPay(cost, handCast.copy(isFromHand = false, isFromExile = true)) shouldBe false
    }

    test("disturb-only mana pays a disturb cast, not a hand cast or a flashback cast") {
        val pool = poolOf(ManaRestriction.DisturbCostOnly)
        pool.canPay(cost, disturbCast) shouldBe true
        pool.canPay(cost, handCast) shouldBe false
        pool.canPay(cost, flashbackCast) shouldBe false
    }

    test("Unblinking Observer's disturb-or-instant/sorcery mana") {
        val pool = poolOf(ManaRestriction.AnyOf(listOf(ManaRestriction.DisturbCostOnly, ManaRestriction.InstantOrSorceryOnly)))
        pool.canPay(cost, disturbCast) shouldBe true
        pool.canPay(cost, handCast) shouldBe true
        pool.canPay(cost, SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true)) shouldBe false
    }

    test("neither restriction pays an ability activation") {
        poolOf(ManaRestriction.CastFromGraveyardOnly).canPay(cost, abilityActivation.copy(isFromGraveyard = true)) shouldBe false
        poolOf(ManaRestriction.DisturbCostOnly).canPay(cost, abilityActivation.copy(isDisturbCast = true)) shouldBe false
    }
})
