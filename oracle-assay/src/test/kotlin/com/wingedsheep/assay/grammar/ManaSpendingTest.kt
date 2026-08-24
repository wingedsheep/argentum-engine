package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Spend this mana only to …" — the spend-restriction sentence, and the two add-clause spellings the
 * family sits behind.
 *
 * The round trip is the property, so most of these assert both directions at once: a line that
 * reads and prints back byte-exactly cannot be reading it wrong in a way the model records. The
 * cases that need more than that are the three the design argues about — the declared empty cell,
 * the fold guard on the join, and the fail-closed strip.
 */
class ManaSpendingTest : StringSpec({

    fun restrictionOf(line: String): ManaRestriction? {
        val outcome = Grammar.abilityLine.parseLine(line)
        val fragment = (outcome as? ParseOutcome.Accepted)?.value ?: return null
        val ability = fragment.script.activatedAbilities.singleOrNull() ?: return null
        return when (val effect = ability.effect) {
            is AddManaEffect -> effect.restriction
            is AddColorlessManaEffect -> effect.restriction
            is AddManaOfChoiceEffect -> effect.restriction
            is AddDynamicManaEffect -> effect.restriction
            else -> null
        }
    }

    fun roundTrips(line: String) {
        val outcome = Grammar.abilityLine.parseLine(line)
        outcome.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        Grammar.abilityLine.printLine(outcome.value) shouldBe line
    }

    fun manaAbility(effect: com.wingedsheep.sdk.scripting.effects.Effect): CardFragment =
        CardFragment.of(
            CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility(
                        cost = com.wingedsheep.sdk.scripting.AbilityCost.Tap,
                        effect = effect,
                        timing = com.wingedsheep.sdk.scripting.TimingRule.ManaAbility,
                        isManaAbility = true,
                    )
                )
            )
        )

    // ---------------------------------------------------------------------------------------
    // The atoms
    // ---------------------------------------------------------------------------------------

    "the closed contexts round-trip in their plural spelling" {
        listOf(
            "{T}: Add {G}. Spend this mana only to cast creature spells.",
            "{T}: Add {U}. Spend this mana only to cast instant or sorcery spells.",
            "{T}: Add {C}{C}. Spend this mana only to cast legendary spells.",
            "{T}: Add {G}{G}. Spend this mana only to cast kicked spells.",
            "{T}: Add {C}{C}. Spend this mana only to activate abilities.",
            "{T}: Add {R}. Spend this mana only to activate equip abilities.",
            "{T}: Add {C}. Spend this mana only to turn permanents face up.",
            "{T}: Add {U}. Spend this mana only to cast spells from exile.",
        ).forEach(::roundTrips)
    }

    "the singular is read and printed back in the canonical plural" {
        restrictionOf("{T}: Add {G}. Spend this mana only to cast a creature spell.") shouldBe
            ManaRestriction.CreatureSpellsOnly

        val outcome = Grammar.abilityLine.parseLine("{T}: Add {G}. Spend this mana only to cast a creature spell.")
        outcome.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        Grammar.abilityLine.printLine(outcome.value) shouldBe
            "{T}: Add {G}. Spend this mana only to cast creature spells."
    }

    "\"instant and sorcery spells\" is the same atom as \"instant or sorcery spells\"" {
        restrictionOf("{T}: Add {R}{R}. Spend this mana only to cast instant and sorcery spells.") shouldBe
            ManaRestriction.InstantOrSorceryOnly
        restrictionOf("{T}: Add {R}{R}. Spend this mana only to cast instant and/or sorcery spells.") shouldBe
            ManaRestriction.InstantOrSorceryOnly
    }

    // ---------------------------------------------------------------------------------------
    // The card-type product
    // ---------------------------------------------------------------------------------------

    "the card type crosses spells, abilities and both" {
        restrictionOf("{T}: Add {U}. Spend this mana only to cast artifact spells.") shouldBe
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.ARTIFACT, allowSpells = true)
        restrictionOf("{T}: Add {C}{C}. Spend this mana only to activate abilities of artifacts.") shouldBe
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                CardType.ARTIFACT, allowSpells = false, allowAbilities = true,
            )
        restrictionOf(
            "{T}: Add {C}{C}. Spend this mana only to cast artifact spells or activate abilities of artifacts."
        ) shouldBe ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            CardType.ARTIFACT, allowSpells = true, allowAbilities = true,
        )
    }

    "\"noncreature\" is the negated column, not a separate atom" {
        restrictionOf("{T}: Add {U}. Spend this mana only to cast noncreature spells.") shouldBe
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE, negated = true)
    }

    "the four spellings of one card type's abilities are one value" {
        listOf(
            "activate abilities of artifacts",
            "activate abilities of artifact sources",
            "activate an ability of an artifact",
            "activate an ability of an artifact source",
        ).forEach { spelling ->
            restrictionOf("{T}: Add {C}. Spend this mana only to $spelling.") shouldBe
                ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                    CardType.ARTIFACT, allowSpells = false, allowAbilities = true,
                )
        }
    }

    /**
     * The declared empty cell. `CreatureSpellsOnly` owns "cast creature spells", so the card-type
     * row must not print the same words for the equivalent value — otherwise one text has two models
     * and the touchstone's ambiguity count leaves zero.
     */
    "the creature spells-only cell is empty in the card-type row" {
        val creatureSpells = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            CardType.CREATURE, allowSpells = true, allowAbilities = false,
        )
        Grammar.abilityLine.printLine(manaAbility(Effects.AddMana(Color.GREEN, 1, creatureSpells))) shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // The subtype
    // ---------------------------------------------------------------------------------------

    "a subtype list joins with \"or\" and folds every conjunction onto one set" {
        restrictionOf("{T}: Add {W}. Spend this mana only to cast Angel spells.") shouldBe
            ManaRestriction.SubtypeSpellsOnly(setOf("Angel"))
        restrictionOf("{T}: Add {W}{W}. Spend this mana only to cast Aura and/or Equipment spells.") shouldBe
            ManaRestriction.SubtypeSpellsOnly(setOf("Aura", "Equipment"))
        restrictionOf(
            "{T}: Add {R}{R}. Spend this mana only to cast Dwarf, Equipment, and Saga spells."
        ) shouldBe ManaRestriction.SubtypeSpellsOnly(setOf("Dwarf", "Equipment", "Saga"))
    }

    "the article agrees with the subtype it precedes" {
        restrictionOf("{T}: Add {G}. Spend this mana only to cast an Omen spell.") shouldBe
            ManaRestriction.SubtypeSpellsOnly(setOf("Omen"))
        restrictionOf("{T}: Add {G}. Spend this mana only to cast a Dragon spell.") shouldBe
            ManaRestriction.SubtypeSpellsOnly(setOf("Dragon"))
    }

    "a subtype's spells and abilities are the one atom that holds both" {
        listOf(
            "cast Dragon spells or activate abilities of Dragons",
            "cast a Dragon spell or activate an ability of a Dragon source",
            "cast Dragon spells and activate abilities of Dragon sources",
        ).forEach { spelling ->
            restrictionOf("{T}: Add {G}. Spend this mana only to $spelling.") shouldBe
                ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Dragon")
        }
    }

    "the two halves of a subtype pair must name the same subtype" {
        restrictionOf(
            "{T}: Add {G}. Spend this mana only to cast Dragon spells or activate abilities of Elves."
        ) shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // The mana-value floor
    // ---------------------------------------------------------------------------------------

    "the mana-value floor crosses its two clauses" {
        roundTrips(
            "{T}: Add two mana of any one color. Spend this mana only to cast spells with mana value 4 or greater."
        )
        restrictionOf(
            "{T}: Add two mana of any one color. Spend this mana only to cast spells with mana value 5 or greater " +
                "or spells with {X} in their mana costs."
        ) shouldBe ManaRestriction.SpellsWithManaValueAtLeast(5, orXInCost = true)
        restrictionOf(
            "{T}: Add two mana of any one color. Spend this mana only to cast creature spells with mana value 4 " +
                "or greater or creature spells with {X} in their mana costs."
        ) shouldBe ManaRestriction.SpellsWithManaValueAtLeast(4, orXInCost = true, creatureOnly = true)
    }

    // ---------------------------------------------------------------------------------------
    // The join
    // ---------------------------------------------------------------------------------------

    "several contexts become an ordered AnyOf" {
        restrictionOf(
            "{T}: Add {U}. Spend this mana only to cast an enchantment spell, unlock a door, " +
                "or turn a permanent face up."
        ) shouldBe ManaRestriction.AnyOf(
            listOf(
                ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.ENCHANTMENT, allowSpells = true),
                ManaRestriction.UnlockDoorOnly,
                ManaRestriction.TurnPermanentsFaceUpOnly,
            )
        )
        restrictionOf(
            "{T}: Add {R}. Spend this mana only to cast Equipment spells or activate equip abilities."
        ) shouldBe ManaRestriction.AnyOf(
            listOf(ManaRestriction.SubtypeSpellsOnly(setOf("Equipment")), ManaRestriction.EquipAbilityActivationOnly)
        )
    }

    "the repeated \"to\" is a spelling, not a second model" {
        restrictionOf("{T}: Add {R}. Spend this mana only to cast an artifact spell or to activate an ability.") shouldBe
            ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.ARTIFACT, allowSpells = true),
                    ManaRestriction.AbilityActivationOnly,
                ),
            )
    }

    /**
     * The fold guard. Both of these would print the words the combined card-type row prints, so
     * neither may be built — the first from the row's own halves, the second through the empty cell's
     * stand-in atom, which is what Gwenna, Eyes of Gaea found.
     */
    "a join that the combined card-type row can express is refused" {
        val spells = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.ARTIFACT, allowSpells = true)
        val abilities = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            CardType.ARTIFACT, allowSpells = false, allowAbilities = true,
        )
        Grammar.abilityLine.printLine(
            manaAbility(Effects.AddMana(Color.WHITE, 1, ManaRestriction.AnyOf(listOf(spells, abilities))))
        ) shouldBe null

        val creatureAbilities = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            CardType.CREATURE, allowSpells = false, allowAbilities = true,
        )
        Grammar.abilityLine.printLine(
            manaAbility(
                Effects.AddMana(
                    Color.GREEN, 1,
                    ManaRestriction.AnyOf(listOf(ManaRestriction.CreatureSpellsOnly, creatureAbilities)),
                )
            )
        ) shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // The prohibition — a different sentence, not a negated spelling
    // ---------------------------------------------------------------------------------------

    "the negative sentence is its own model" {
        restrictionOf("{T}: Add {C}{C}. This mana can't be spent to cast nonartifact spells.") shouldBe
            ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT))
        restrictionOf("{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.") shouldBe
            ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT))
        // The un-negated spelling names no set the model could hold, so it is not a reading.
        restrictionOf("{T}: Add {C}. This mana can't be spent to cast artifact spells.") shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // The positions the clause inherits, and the add clauses under it
    // ---------------------------------------------------------------------------------------

    "the restriction rides every position the add clause reaches" {
        listOf(
            "Add four mana in any combination of colors. Spend this mana only to cast Dragon spells.",
            "{T}, Pay 1 life: Add {B}. Spend this mana only to cast instant or sorcery spells.",
            "{T}: Add three mana of any one color. Spend this mana only to cast creature spells.",
            "{T}: Add two mana in any combination of colors. Spend this mana only to cast legendary spells.",
        ).forEach(::roundTrips)
    }

    "the choice form carries one restriction onto each of its abilities" {
        val line = "{T}: Add {U} or {R}. Spend this mana only to cast noncreature spells."
        val outcome = Grammar.abilityLine.parseLine(line)
        outcome.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        val abilities = outcome.value.script.activatedAbilities
        abilities.size shouldBe 2
        abilities.map { (it.effect as AddManaEffect).color } shouldBe listOf(Color.BLUE, Color.RED)
        abilities.forEach {
            (it.effect as AddManaEffect).restriction shouldBe
                ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE, negated = true)
        }
        Grammar.abilityLine.printLine(outcome.value) shouldBe line
    }

    /**
     * "Add two mana of any **one** color" is what 63 corpus lines print and "of any color" is what 3
     * print; the rule used to spell only the second. The singular takes the pair the other way round.
     */
    "the plural any-colour clause is spelled \"any one color\"" {
        roundTrips("{T}: Add two mana of any one color.")
        roundTrips("{T}: Add one mana of any color.")

        val alternate = Grammar.abilityLine.parseLine("{T}: Add two mana of any color.")
        alternate.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        Grammar.abilityLine.printLine(alternate.value) shouldBe "{T}: Add two mana of any one color."

        val singularAlternate = Grammar.abilityLine.parseLine("{T}: Add one mana of any one color.")
        singularAlternate.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        Grammar.abilityLine.printLine(singularAlternate.value) shouldBe "{T}: Add one mana of any color."
    }

    "\"in any combination of colors\" is every colour, and the enumerated form still reads" {
        val outcome = Grammar.abilityLine.parseLine("{T}: Add two mana in any combination of colors.")
        outcome.shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>()
        val effect = outcome.value.script.activatedAbilities.single().effect as AddDynamicManaEffect
        effect.allowedColors shouldBe Color.entries.toSet()
        effect.amountSource shouldBe DynamicAmount.Fixed(2)
        roundTrips("{T}: Add three mana in any combination of {R} and/or {G}.")
    }

    /**
     * The strip is fail-closed in both directions: an effect carrying anything the add clause cannot
     * say — here a rider — must refuse to print rather than printing the bare sentence and dropping
     * it. That property lives in the inner clauses, and this is the test that it still holds through
     * the restricted wrapper.
     */
    "an effect carrying a rider refuses to print" {
        val withRider = AddManaEffect(
            color = Color.GREEN,
            amount = DynamicAmount.Fixed(1),
            restriction = ManaRestriction.CreatureSpellsOnly,
            riders = setOf(com.wingedsheep.sdk.scripting.effects.ManaSpellRider.MakesSpellUncounterable),
        )
        Grammar.abilityLine.printLine(manaAbility(withRider)) shouldBe null
    }
})
