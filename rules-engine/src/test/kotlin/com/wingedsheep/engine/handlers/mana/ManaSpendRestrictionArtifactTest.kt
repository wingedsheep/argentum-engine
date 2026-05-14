package com.wingedsheep.engine.handlers.mana

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD tests for the two restrictions printed on Steelswarm Operator:
 *   - [ManaRestriction.ArtifactSpellsOnly] (spend only to cast an artifact spell)
 *   - [ManaRestriction.ArtifactSourceAbilitiesOnly] (spend only to activate abilities of
 *     artifact sources — any zone)
 */
class ManaSpendRestrictionArtifactTest : FunSpec({

    test("ArtifactSpellsOnly mana pays for an artifact spell but not a non-artifact spell") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, ManaRestriction.ArtifactSpellsOnly)

        val artifactContext = SpellPaymentContext(isArtifact = true)
        val creatureOnlyContext = SpellPaymentContext(isCreature = true, isArtifact = false)

        pool.canPay(cost, artifactContext) shouldBe true
        pool.canPay(cost, creatureOnlyContext) shouldBe false
    }

    test("ArtifactSpellsOnly mana is rejected for ability activations") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, ManaRestriction.ArtifactSpellsOnly)

        val abilityFromArtifact = SpellPaymentContext(
            isAbilityActivation = true,
            isAbilityFromArtifactSource = true,
        )

        pool.canPay(cost, abilityFromArtifact) shouldBe false
    }

    test("ArtifactSourceAbilitiesOnly mana pays for abilities of artifact sources only") {
        val cost = ManaCost.parse("{U}{U}")
        val pool = ManaPool()
            .addRestricted(Color.BLUE, 1, ManaRestriction.ArtifactSourceAbilitiesOnly)
            .addRestricted(Color.BLUE, 1, ManaRestriction.ArtifactSourceAbilitiesOnly)

        val artifactAbility = SpellPaymentContext(
            isAbilityActivation = true,
            isAbilityFromArtifactSource = true,
        )
        val nonArtifactAbility = SpellPaymentContext(
            isAbilityActivation = true,
            isAbilityFromArtifactSource = false,
        )

        pool.canPay(cost, artifactAbility) shouldBe true
        pool.canPay(cost, nonArtifactAbility) shouldBe false
    }

    test("ArtifactSourceAbilitiesOnly mana cannot be spent on casting any spell") {
        val cost = ManaCost.parse("{U}")
        val pool = ManaPool().addRestricted(Color.BLUE, 1, ManaRestriction.ArtifactSourceAbilitiesOnly)

        val artifactSpell = SpellPaymentContext(isArtifact = true)
        val nonArtifactSpell = SpellPaymentContext(isCreature = true)

        pool.canPay(cost, artifactSpell) shouldBe false
        pool.canPay(cost, nonArtifactSpell) shouldBe false
    }
})
