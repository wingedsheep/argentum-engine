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
 * Tests for [ManaRestriction.AbilityActivationOnly] — "spend this mana only to activate an
 * ability" (any ability of any source) — and the Purple Dragon Punks composition
 * (`AnyOf(artifact spells, any ability)` for "cast an artifact spell or activate an ability").
 */
class ManaSpendRestrictionAnyAbilityTest : FunSpec({

    val cost = ManaCost.parse("{R}")

    // Purple Dragon Punks: "Spend this mana only to cast an artifact spell or to activate an ability."
    val punks = ManaRestriction.AnyOf(
        listOf(
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT, allowSpells = true, allowAbilities = false,
            ),
            ManaRestriction.AbilityActivationOnly,
        ),
    )

    test("any-ability mana pays for an ability of a NON-artifact source") {
        val pool = ManaPool().addRestricted(Color.RED, 1, ManaRestriction.AbilityActivationOnly)
        // A creature's activated ability — CardTypeSpellsOrAbilitiesOnly(ARTIFACT) would reject this.
        pool.canPay(
            cost,
            SpellPaymentContext(isAbilityActivation = true, abilitySourceCardTypes = setOf(CardType.CREATURE)),
        ) shouldBe true
    }

    test("any-ability mana is rejected for a spell cast") {
        val pool = ManaPool().addRestricted(Color.RED, 1, ManaRestriction.AbilityActivationOnly)
        pool.canPay(cost, SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))) shouldBe false
    }

    test("Purple Dragon Punks mana pays for an artifact spell") {
        val pool = ManaPool().addRestricted(Color.RED, 1, punks)
        pool.canPay(cost, SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))) shouldBe true
    }

    test("Purple Dragon Punks mana pays for any ability activation") {
        val pool = ManaPool().addRestricted(Color.RED, 1, punks)
        pool.canPay(
            cost,
            SpellPaymentContext(isAbilityActivation = true, abilitySourceCardTypes = setOf(CardType.CREATURE)),
        ) shouldBe true
    }

    test("Purple Dragon Punks mana does NOT pay for a non-artifact spell") {
        val pool = ManaPool().addRestricted(Color.RED, 1, punks)
        pool.canPay(cost, SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))) shouldBe false
        pool.canPay(cost, SpellPaymentContext(isInstantOrSorcery = true, cardTypes = setOf(CardType.INSTANT))) shouldBe false
    }
})
