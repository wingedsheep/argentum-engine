package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.CLOAK
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.DISGUISE
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.MANIFEST
import com.wingedsheep.sdk.scripting.effects.FaceDownMode.MORPH
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.TurnPermanentsFaceUpOnly]'s `procedures` axis — the turn-up procedure being paid
 * for — against every turn-up route the engine builds ([SpellPaymentContext.turnFaceUp]) and the
 * non-turn-up payments.
 */
class TurnFaceUpRouteManaRestrictionTest : FunSpec({

    val routes = listOf(MORPH, DISGUISE, MANIFEST, CLOAK).map { SpellPaymentContext.turnFaceUp(it) }
    val notTurnUps = listOf(
        SpellPaymentContext.faceDownCast(),
        SpellPaymentContext(isAbilityActivation = true),
        SpellPaymentContext(isUnlockDoorAction = true),
        SpellPaymentContext(isCreature = true),
    )

    test("without procedures every turn-up route qualifies, and nothing else does") {
        val any = ManaRestriction.TurnPermanentsFaceUpOnly()
        routes.forEach { any.isSatisfiedBy(it) shouldBe true }
        notTurnUps.forEach { any.isSatisfiedBy(it) shouldBe false }
        any.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe true
    }

    test("procedures admits only the named turn-up procedures") {
        val qarsi = ManaRestriction.TurnPermanentsFaceUpOnly(procedures = setOf(MANIFEST, MORPH))
        qarsi.isSatisfiedBy(SpellPaymentContext.turnFaceUp(MORPH)) shouldBe true
        qarsi.isSatisfiedBy(SpellPaymentContext.turnFaceUp(MANIFEST)) shouldBe true
        qarsi.isSatisfiedBy(SpellPaymentContext.turnFaceUp(DISGUISE)) shouldBe false
        qarsi.isSatisfiedBy(SpellPaymentContext.turnFaceUp(CLOAK)) shouldBe false
        // A turn-up context that records no procedure can't satisfy a narrowed restriction.
        qarsi.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe false
        notTurnUps.forEach { qarsi.isSatisfiedBy(it) shouldBe false }
    }

    test("descriptions") {
        ManaRestriction.TurnPermanentsFaceUpOnly().description shouldBe
            "Spend this mana only to turn permanents face up"
        ManaRestriction.TurnPermanentsFaceUpOnly(setOf(FaceDownMode.MORPH)).description shouldBe
            "Spend this mana only to turn permanents face up by morph"
    }
})
