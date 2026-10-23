package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level tests for [ManaRestriction.ClassLevelUpOnly] — "Spend this mana only to … gain a
 * Class level" (CR 716.2c: activating an ability indicated by a class level bar).
 *
 * The restriction reads [SpellPaymentContext.isClassLevelUpActivation], a fact about the *ability*
 * rather than its source: a Class's level-up and the other activated abilities its levels grant
 * share one enchantment source, so no source type/subtype test separates them. The end-to-end
 * payment (floating Sorcerer Class mana paying a real level-up, refusing a creature spell) lives in
 * `SorcererClassScenarioTest`.
 */
class ClassLevelUpManaRestrictionTest : FunSpec({

    val levelUp = SpellPaymentContext(
        isAbilityActivation = true,
        isClassLevelUpActivation = true,
        abilitySourceCardTypes = setOf(CardType.ENCHANTMENT),
        subtypes = setOf("Class"),
    )
    val classesOtherAbility = SpellPaymentContext(
        isAbilityActivation = true,
        abilitySourceCardTypes = setOf(CardType.ENCHANTMENT),
        subtypes = setOf("Class"),
    )
    val equip = SpellPaymentContext(
        isAbilityActivation = true,
        isEquipAbilityActivation = true,
        abilitySourceCardTypes = setOf(CardType.ARTIFACT),
        subtypes = setOf("Equipment"),
    )
    val instant = SpellPaymentContext(isInstantOrSorcery = true, cardTypes = setOf(CardType.INSTANT))
    val classSpell = SpellPaymentContext(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf("Class"))
    val creatureSpell = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))

    test("only a Class level-up activation satisfies it") {
        val restriction = ManaRestriction.ClassLevelUpOnly
        restriction.isSatisfiedBy(levelUp) shouldBe true
        restriction.isSatisfiedBy(classesOtherAbility) shouldBe false
        restriction.isSatisfiedBy(equip) shouldBe false
        restriction.isSatisfiedBy(instant) shouldBe false
        restriction.isSatisfiedBy(classSpell) shouldBe false
        restriction.isSatisfiedBy(creatureSpell) shouldBe false
        restriction.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe false
        restriction.isSatisfiedBy(SpellPaymentContext(isUnlockDoorAction = true)) shouldBe false

        // The source-keyed spelling admits the Class's other abilities — why the atom exists.
        ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Class").isSatisfiedBy(classesOtherAbility) shouldBe true
    }

    test("Sorcerer Class's composed restriction pays instants, sorceries and level-ups only") {
        val restriction = ManaRestriction.AnyOf(
            listOf(ManaRestriction.InstantOrSorceryOnly, ManaRestriction.ClassLevelUpOnly)
        )
        val pool = ManaPool(restrictedMana = List(2) { RestrictedManaEntry(Color.BLUE, restriction) })
        val cost = ManaCost.parse("{1}")
        pool.canPay(cost, instant) shouldBe true
        pool.canPay(cost, levelUp) shouldBe true
        pool.canPay(cost, classesOtherAbility) shouldBe false
        pool.canPay(cost, classSpell) shouldBe false
        pool.canPay(cost, creatureSpell) shouldBe false
        pool.canPay(cost, null) shouldBe false
    }

    test("the level-up flag may not be set without an ability activation") {
        shouldThrow<IllegalArgumentException> { SpellPaymentContext(isClassLevelUpActivation = true) }
    }

    test("the restriction renders its printed clause") {
        ManaRestriction.ClassLevelUpOnly.description shouldBe "Spend this mana only to gain a Class level"
    }
})
