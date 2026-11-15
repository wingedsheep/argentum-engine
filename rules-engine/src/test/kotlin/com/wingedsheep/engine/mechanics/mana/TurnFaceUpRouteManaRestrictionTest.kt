package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.CLOAK
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.DISGUISE
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.HIDDEN
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.MANIFEST
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.MORPH
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.TurnPermanentsFaceUpOnly]'s two narrowing axes — the turn-up procedure paid for
 * (`procedures`) and how the permanent went face down (`faceDownAs`) — against every turn-up route
 * the engine builds ([SpellPaymentContext.turnFaceUp]) and the non-turn-up payments.
 */
class TurnFaceUpRouteManaRestrictionTest : FunSpec({

    fun up(procedure: FaceDownMode, faceDownAs: FaceDownMode?) = SpellPaymentContext.turnFaceUp(procedure, faceDownAs)

    val routes = listOf(
        up(MORPH, MORPH), up(DISGUISE, DISGUISE), up(MANIFEST, MANIFEST), up(CLOAK, CLOAK),
        up(MORPH, MANIFEST), up(DISGUISE, CLOAK), up(MORPH, HIDDEN), up(MORPH, null),
    )
    val notTurnUps = listOf(
        SpellPaymentContext.faceDownCast(),
        SpellPaymentContext(isAbilityActivation = true),
        SpellPaymentContext(isUnlockDoorAction = true),
        SpellPaymentContext(isCreature = true),
    )

    test("with neither axis set every turn-up route qualifies, and nothing else does") {
        val any = ManaRestriction.TurnPermanentsFaceUpOnly()
        routes.forEach { any.isSatisfiedBy(it) shouldBe true }
        notTurnUps.forEach { any.isSatisfiedBy(it) shouldBe false }
        // The bare flag (no route recorded) still reads as a turn-up for the unnarrowed atom.
        any.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe true
    }

    test("procedures narrows to the cost being paid, whatever put the permanent face down") {
        val morphCost = ManaRestriction.TurnPermanentsFaceUpOnly(procedures = setOf(MORPH))
        morphCost.isSatisfiedBy(up(MORPH, MORPH)) shouldBe true
        morphCost.isSatisfiedBy(up(MORPH, MANIFEST)) shouldBe true // CR 701.40c: manifested card with morph
        morphCost.isSatisfiedBy(up(MORPH, HIDDEN)) shouldBe true
        morphCost.isSatisfiedBy(up(MANIFEST, MANIFEST)) shouldBe false
        morphCost.isSatisfiedBy(up(DISGUISE, DISGUISE)) shouldBe false
        morphCost.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe false
        notTurnUps.forEach { morphCost.isSatisfiedBy(it) shouldBe false }
    }

    test("faceDownAs narrows to how the permanent went face down, whatever procedure turns it up") {
        val manifested = ManaRestriction.TurnPermanentsFaceUpOnly(faceDownAs = setOf(MANIFEST))
        manifested.isSatisfiedBy(up(MANIFEST, MANIFEST)) shouldBe true
        manifested.isSatisfiedBy(up(MORPH, MANIFEST)) shouldBe true
        manifested.isSatisfiedBy(up(CLOAK, CLOAK)) shouldBe false
        manifested.isSatisfiedBy(up(MORPH, MORPH)) shouldBe false
        manifested.isSatisfiedBy(up(MORPH, null)) shouldBe false
        notTurnUps.forEach { manifested.isSatisfiedBy(it) shouldBe false }
    }

    test("both axes set must both hold") {
        val both = ManaRestriction.TurnPermanentsFaceUpOnly(procedures = setOf(MANIFEST), faceDownAs = setOf(MANIFEST))
        both.isSatisfiedBy(up(MANIFEST, MANIFEST)) shouldBe true
        both.isSatisfiedBy(up(MORPH, MANIFEST)) shouldBe false
        both.isSatisfiedBy(up(CLOAK, CLOAK)) shouldBe false
    }

    test("descriptions read as the printed clause") {
        ManaRestriction.TurnPermanentsFaceUpOnly().description shouldBe
            "Spend this mana only to turn permanents face up"
        ManaRestriction.TurnPermanentsFaceUpOnly(procedures = setOf(MORPH)).description shouldBe
            "Spend this mana only to pay a morph cost"
        ManaRestriction.TurnPermanentsFaceUpOnly(faceDownAs = setOf(MANIFEST)).description shouldBe
            "Spend this mana only to turn a manifested creature face up"
    }
})
