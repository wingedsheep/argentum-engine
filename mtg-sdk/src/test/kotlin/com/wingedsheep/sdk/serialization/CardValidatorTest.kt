package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for post-deserialization card validation.
 */
class CardValidatorTest : DescribeSpec({

    val vanillaCreature = CardDefinition(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Bear"))),
        creatureStats = CreatureStats(2, 2),
    )

    describe("valid cards") {

        it("returns no errors for a simple creature") {
            CardValidator.validate(vanillaCreature).shouldBeEmpty()
        }

        it("returns no errors for an instant with a target") {
            val card = CardDefinition(
                name = "Lightning Bolt",
                manaCost = ManaCost.parse("{R}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = DealDamageEffect(
                        amount = DynamicAmount.Fixed(3),
                        target = EffectTarget.ContextTarget(0),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            CardValidator.validate(card).shouldBeEmpty()
        }

        it("returns no errors for an Aura with auraTarget") {
            val card = CardDefinition(
                name = "Holy Strength",
                manaCost = ManaCost.parse("{W}"),
                typeLine = TypeLine.aura(),
                script = CardScript(auraTarget = TargetCreature()),
            )
            CardValidator.validate(card).shouldBeEmpty()
        }

        it("returns no errors for Equipment with equipCost") {
            val card = CardDefinition(
                name = "Short Sword",
                manaCost = ManaCost.parse("{1}"),
                typeLine = TypeLine.equipment(),
                equipCost = ManaCost.parse("{1}"),
            )
            CardValidator.validate(card).shouldBeEmpty()
        }

        it("returns no errors for a planeswalker with loyalty") {
            val card = CardDefinition(
                name = "Jace",
                manaCost = ManaCost.parse("{2}{U}{U}"),
                typeLine = TypeLine(
                    supertypes = setOf(Supertype.LEGENDARY),
                    cardTypes = setOf(CardType.PLANESWALKER),
                    subtypes = setOf(Subtype("Jace")),
                ),
                startingLoyalty = 3,
            )
            CardValidator.validate(card).shouldBeEmpty()
        }
    }

    describe("InvalidTargetIndex") {

        it("flags a spell effect that references a non-existent target index") {
            val card = CardDefinition(
                name = "Broken Bolt",
                manaCost = ManaCost.parse("{R}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = DealDamageEffect(
                        amount = DynamicAmount.Fixed(3),
                        target = EffectTarget.ContextTarget(5),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            val err = errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>()
            err.index shouldBe 5
            err.maxIndex shouldBe 0
            err.cardName shouldBe "Broken Bolt"
        }

        it("flags a spell with targets referenced inside a CompositeEffect") {
            val card = CardDefinition(
                name = "Composite Bolt",
                manaCost = ManaCost.parse("{1}{R}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = CompositeEffect(listOf(
                        DrawCardsEffect(count = DynamicAmount.Fixed(1), target = EffectTarget.Controller),
                        DealDamageEffect(DynamicAmount.Fixed(2), EffectTarget.ContextTarget(2)),
                    )),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>().index shouldBe 2
        }

        it("flags nested MayEffect inside ConditionalEffect + elseEffect") {
            val card = CardDefinition(
                name = "Deep Nest",
                manaCost = ManaCost.parse("{2}{R}"),
                typeLine = TypeLine.sorcery(),
                script = CardScript(
                    spellEffect = ConditionalEffect(
                        condition = SourceIsAttacking,
                        effect = MayEffect(
                            DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(3))
                        ),
                        elseEffect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(7)),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 2
            errors.map { (it as CardValidationError.InvalidTargetIndex).index }.sorted() shouldBe listOf(3, 7)
        }

        it("flags targeted triggered abilities with a bad target index") {
            val card = CardDefinition(
                name = "Strange ETB",
                manaCost = ManaCost.parse("{2}{R}"),
                typeLine = TypeLine.creature(),
                creatureStats = CreatureStats(2, 2),
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = GameEvent.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                            effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(2)),
                            targetRequirement = AnyTarget(),
                        ),
                    ),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            val err = errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>()
            err.index shouldBe 2
            // Only one target slot on triggered abilities
            err.maxIndex shouldBe 0
        }

        it("flags triggered abilities that reference any ContextTarget when no target is declared") {
            val card = CardDefinition(
                name = "Bad Trigger",
                manaCost = ManaCost.parse("{1}{R}"),
                typeLine = TypeLine.creature(),
                creatureStats = CreatureStats(1, 1),
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = GameEvent.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                            effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                        ),
                    ),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>().maxIndex shouldBe -1
        }

        it("collects indices inside ModalEffect modes") {
            val card = CardDefinition(
                name = "Modal Charm",
                manaCost = ManaCost.parse("{1}{R}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = ModalEffect(
                        chooseCount = 1,
                        modes = listOf(
                            Mode(
                                description = "Mode A",
                                effect = DealDamageEffect(
                                    DynamicAmount.Fixed(2),
                                    EffectTarget.ContextTarget(0),
                                ),
                            ),
                            Mode(
                                description = "Mode B",
                                effect = ModifyStatsEffect(
                                    powerModifier = DynamicAmount.Fixed(1),
                                    toughnessModifier = DynamicAmount.Fixed(1),
                                    target = EffectTarget.ContextTarget(4),
                                ),
                            ),
                        ),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>().index shouldBe 4
        }

        it("accepts ContextTarget(0) with exactly one target declared") {
            val card = CardDefinition(
                name = "Ok Bolt",
                manaCost = ManaCost.parse("{R}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = MayEffect(
                        DealDamageEffect(DynamicAmount.Fixed(3), EffectTarget.ContextTarget(0)),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            CardValidator.validate(card).shouldBeEmpty()
        }
    }

    describe("Aura consistency") {

        it("flags an Aura subtype without an auraTarget in the script") {
            val card = CardDefinition(
                name = "Silent Aura",
                manaCost = ManaCost.parse("{W}"),
                typeLine = TypeLine.aura(),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            val err = errors[0].shouldBeInstanceOf<CardValidationError.AuraMissingTarget>()
            err.cardName shouldBe "Silent Aura"
            err.message shouldContain "Aura subtype"
        }

        it("flags an auraTarget on a card that isn't an Aura") {
            val card = CardDefinition(
                name = "Non-Aura Enchantment",
                manaCost = ManaCost.parse("{W}"),
                typeLine = TypeLine.enchantment(),
                script = CardScript(auraTarget = TargetCreature()),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.AuraMissingSubtype>().cardName shouldBe "Non-Aura Enchantment"
        }
    }

    describe("Equipment consistency") {

        it("flags an equipCost on a card that isn't Equipment") {
            val card = CardDefinition(
                name = "Weird Artifact",
                manaCost = ManaCost.parse("{1}"),
                typeLine = TypeLine.artifact(),
                equipCost = ManaCost.parse("{1}"),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.EquipmentMissingSubtype>()
        }
    }

    describe("Planeswalker loyalty") {

        it("flags a planeswalker without startingLoyalty") {
            val card = CardDefinition(
                name = "Loyalty-Less Planeswalker",
                manaCost = ManaCost.parse("{3}{U}"),
                typeLine = TypeLine(
                    supertypes = setOf(Supertype.LEGENDARY),
                    cardTypes = setOf(CardType.PLANESWALKER),
                    subtypes = setOf(Subtype("Tester")),
                ),
                // startingLoyalty intentionally null
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.MissingPlaneswalkerLoyalty>()
        }
    }

    describe("MoveToZoneEffect target indexing") {

        it("validates effects with MoveToZone (destroy)") {
            val card = CardDefinition(
                name = "Doom Bolt",
                manaCost = ManaCost.parse("{1}{B}"),
                typeLine = TypeLine.instant(),
                script = CardScript(
                    spellEffect = MoveToZoneEffect(
                        target = EffectTarget.ContextTarget(9),
                        destination = Zone.GRAVEYARD,
                        byDestruction = true,
                    ),
                    targetRequirements = listOf(TargetCreature()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>().index shouldBe 9
        }
    }

    describe("multiple independent errors") {

        it("collects errors from several issues at once") {
            val card = CardDefinition(
                name = "Messy Card",
                manaCost = ManaCost.parse("{W}"),
                typeLine = TypeLine.aura(),
                equipCost = ManaCost.parse("{1}"),  // equipment cost on non-equipment
                script = CardScript(
                    // auraTarget missing → AuraMissingTarget
                    spellEffect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(3)),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val errors = CardValidator.validate(card)
            errors shouldHaveSize 3
            errors.any { it is CardValidationError.AuraMissingTarget } shouldBe true
            errors.any { it is CardValidationError.EquipmentMissingSubtype } shouldBe true
            errors.any { it is CardValidationError.InvalidTargetIndex } shouldBe true
        }
    }
})
