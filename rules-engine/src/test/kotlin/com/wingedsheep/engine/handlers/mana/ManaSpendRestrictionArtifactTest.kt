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
 * BDD tests for [ManaRestriction.CardTypeSpellsOrAbilitiesOnly] in the three printed
 * shapes (spells only / abilities only / both), exercised against the artifact-typed
 * variant printed on Steelswarm Operator.
 */
class ManaSpendRestrictionArtifactTest : FunSpec({

    val artifactSpellsOnly = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
        cardType = CardType.ARTIFACT, allowSpells = true, allowAbilities = false,
    )
    val artifactAbilitiesOnly = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
        cardType = CardType.ARTIFACT, allowSpells = false, allowAbilities = true,
    )
    val artifactSpellsOrAbilities = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
        cardType = CardType.ARTIFACT, allowSpells = true, allowAbilities = true,
    )

    test("artifact-spells-only mana pays for an artifact spell but not a non-artifact spell") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, artifactSpellsOnly)

        val artifactSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))
        val creatureSpell = SpellPaymentContext(
            isCreature = true,
            cardTypes = setOf(CardType.CREATURE),
        )

        pool.canPay(cost, artifactSpell) shouldBe true
        pool.canPay(cost, creatureSpell) shouldBe false
    }

    test("artifact-spells-only mana is rejected for ability activations") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, artifactSpellsOnly)

        val abilityFromArtifact = SpellPaymentContext(
            isAbilityActivation = true,
            abilitySourceCardTypes = setOf(CardType.ARTIFACT),
        )

        pool.canPay(cost, abilityFromArtifact) shouldBe false
    }

    test("artifact-abilities-only mana pays for abilities of artifact sources only") {
        val cost = ManaCost.parse("{U}{U}")
        val pool = ManaPool()
            .addRestricted(Color.BLUE, 1, artifactAbilitiesOnly)
            .addRestricted(Color.BLUE, 1, artifactAbilitiesOnly)

        val artifactAbility = SpellPaymentContext(
            isAbilityActivation = true,
            abilitySourceCardTypes = setOf(CardType.ARTIFACT),
        )
        val nonArtifactAbility = SpellPaymentContext(
            isAbilityActivation = true,
            abilitySourceCardTypes = setOf(CardType.CREATURE),
        )

        pool.canPay(cost, artifactAbility) shouldBe true
        pool.canPay(cost, nonArtifactAbility) shouldBe false
    }

    test("artifact-abilities-only mana cannot be spent on casting any spell") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, artifactAbilitiesOnly)

        val artifactSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))
        val creatureSpell = SpellPaymentContext(
            isCreature = true,
            cardTypes = setOf(CardType.CREATURE),
        )

        pool.canPay(cost, artifactSpell) shouldBe false
        pool.canPay(cost, creatureSpell) shouldBe false
    }

    test("both-allowed variant accepts spells and ability activations of the type") {
        val cost = ManaCost.parse("{U}")
        fun freshPool() = ManaPool().addRestricted(Color.BLUE, 1, artifactSpellsOrAbilities)

        val artifactSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))
        val artifactAbility = SpellPaymentContext(
            isAbilityActivation = true,
            abilitySourceCardTypes = setOf(CardType.ARTIFACT),
        )
        val creatureSpell = SpellPaymentContext(
            isCreature = true,
            cardTypes = setOf(CardType.CREATURE),
        )

        freshPool().canPay(cost, artifactSpell) shouldBe true
        freshPool().canPay(cost, artifactAbility) shouldBe true
        freshPool().canPay(cost, creatureSpell) shouldBe false
    }

    test("constructor rejects a useless restriction allowing neither spells nor abilities") {
        runCatching {
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = false,
                allowAbilities = false,
            )
        }.isFailure shouldBe true
    }
})
