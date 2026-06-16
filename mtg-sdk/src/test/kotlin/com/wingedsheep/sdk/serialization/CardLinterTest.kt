package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for [CardLinter] — the structural dataflow lint (sdk-analysis §1.1).
 * The corpus-wide gate lives in mtg-sets (`CardLintTest`); these tests pin the
 * individual checks on minimal synthetic cards.
 */
class CardLinterTest : DescribeSpec({

    fun instant(name: String, script: CardScript) = CardDefinition(
        name = name,
        manaCost = ManaCost.parse("{1}{U}"),
        typeLine = TypeLine.instant(),
        script = script,
    )

    fun gather(storeAs: String) =
        GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)), storeAs = storeAs)

    fun move(from: String) =
        MoveCollectionEffect(from = from, destination = CardDestination.ToZone(Zone.HAND))

    describe("pipeline dataflow") {

        it("accepts a well-formed gather → select → move pipeline") {
            val card = instant(
                "Clean Pipeline",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(
                            gather("looked"),
                            SelectFromCollectionEffect(
                                from = "looked",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                storeSelected = "kept",
                                storeRemainder = "rest",
                            ),
                            move("kept"),
                            MoveCollectionEffect(
                                from = "rest",
                                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                            ),
                        ),
                    ),
                ),
            )
            CardLinter.lint(card).shouldBeEmpty()
        }

        it("flags a typo'd collection read as an error with a suggestion") {
            val card = instant(
                "Typo Pipeline",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(gather("revealed"), move("revaeled")),
                    ),
                ),
            )
            val findings = CardLinter.lint(card)
            // The unread write and the unresolved read are two views of the same typo.
            val errors = findings.filter { it.severity == LintSeverity.ERROR }
            errors.shouldHaveSize(1)
            errors[0].shouldBeInstanceOf<CardValidationError.UnresolvedPipelineRead>()
                .message shouldContain "'revealed'"
        }

        it("flags a read whose writer lives in a different ability as a warning") {
            val card = CardDefinition(
                name = "Split Flow",
                manaCost = ManaCost.parse("{2}"),
                typeLine = TypeLine.creature(),
                creatureStats = CreatureStats(2, 2),
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                            effect = gather("stash"),
                        ),
                        TriggeredAbility.create(
                            trigger = EventPattern.ZoneChangeEvent(
                                from = Zone.BATTLEFIELD,
                                to = Zone.GRAVEYARD,
                            ),
                            effect = move("stash"),
                        ),
                    ),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.filter { it.severity == LintSeverity.ERROR }.shouldBeEmpty()
            findings.filterIsInstance<CardValidationError.CrossScopePipelineRead>().shouldHaveSize(1)
        }

        it("flags a store nothing reads as a warning") {
            val card = instant(
                "Hoarder",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(
                            gather("looked"),
                            SelectFromCollectionEffect(
                                from = "looked",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                storeSelected = "kept",
                                storeRemainder = "forgotten",
                            ),
                            move("kept"),
                        ),
                    ),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.filter { it.severity == LintSeverity.ERROR }.shouldBeEmpty()
            findings.filterIsInstance<CardValidationError.OrphanPipelineWrite>()
                .shouldHaveSize(1).first().message shouldContain "'forgotten'"
        }

        it("connects cast-time additional-cost stores to the spell effect") {
            val card = instant(
                "Behold Payoff",
                CardScript(
                    spellEffect = move("beheld"),
                    additionalCosts = listOf(AdditionalCost.Behold()),
                ),
            )
            CardLinter.lint(card).filter { it.severity == LintSeverity.ERROR }.shouldBeEmpty()
        }

        it("bridges a collection write to its _count numeric read") {
            val card = instant(
                "Counter",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(
                            gather("exiled"),
                            move("exiled"),
                            DealDamageEffect(
                                DynamicAmount.VariableReference("exiled_count"),
                                EffectTarget.PlayerRef(
                                    com.wingedsheep.sdk.scripting.references.Player.EachOpponent
                                ),
                            ),
                        ),
                    ),
                ),
            )
            CardLinter.lint(card).filter { it.severity == LintSeverity.ERROR }.shouldBeEmpty()
        }
    }

    describe("target bindings per owning scope") {

        it("scopes a granted ability's ContextTarget to the granted ability") {
            // The granted ability declares no targets; ContextTarget(0) inside it must NOT
            // resolve against the outer spell's requirement.
            val card = instant(
                "Bad Grant",
                CardScript(
                    spellEffect = GrantTriggeredAbilityEffect(
                        ability = TriggeredAbility.create(
                            trigger = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
                            effect = DealDamageEffect(
                                DynamicAmount.Fixed(1),
                                EffectTarget.ContextTarget(0),
                            ),
                        ),
                        target = EffectTarget.ContextTarget(0),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.shouldHaveSize(1)
            findings[0].shouldBeInstanceOf<CardValidationError.InvalidTargetIndex>()
                .message shouldContain "granted"
        }

        it("flags a BoundVariable that matches no requirement id") {
            val card = instant(
                "Named Wrong",
                CardScript(
                    spellEffect = DealDamageEffect(
                        DynamicAmount.Fixed(2),
                        EffectTarget.BoundVariable("victim"),
                    ),
                    targetRequirements = listOf(AnyTarget(id = "target")),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.shouldHaveSize(1)
            findings[0].shouldBeInstanceOf<CardValidationError.UnknownTargetBinding>()
                .message shouldContain "'victim'"
        }

        it("accepts indexed BoundVariable names against the base id") {
            val card = instant(
                "Named Right",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(
                            DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.BoundVariable("creature[0]")),
                            DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.BoundVariable("creature[1]")),
                        ),
                    ),
                    targetRequirements = listOf(AnyTarget(id = "creature")),
                ),
            )
            CardLinter.lint(card).shouldBeEmpty()
        }
    }

    describe("choice slots") {

        it("flags a slot read with no declaration") {
            val card = instant(
                "Unkicked",
                CardScript(
                    spellEffect = ConditionalEffect(
                        condition = CastChoiceMade(ChoiceSlot.KICKED),
                        effect = DealDamageEffect(DynamicAmount.Fixed(3), EffectTarget.ContextTarget(0)),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.shouldHaveSize(1)
            findings[0].shouldBeInstanceOf<CardValidationError.UndeclaredChoiceSlotRead>()
                .message shouldContain "KICKED"
        }

        it("accepts a KICKED read when a kicker effect is declared") {
            val card = instant(
                "Kicked",
                CardScript(
                    spellEffect = ConditionalEffect(
                        condition = CastChoiceMade(ChoiceSlot.KICKED),
                        effect = DealDamageEffect(DynamicAmount.Fixed(3), EffectTarget.ContextTarget(0)),
                    ),
                    kickerSpellEffect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            CardLinter.lint(card).shouldBeEmpty()
        }
    }

    describe("opponent-chosen targets") {

        it("flags a TargetChooser.Opponent target on a spell — the controller would pick it") {
            val card = instant(
                "Wrongly Opponent-Chosen",
                CardScript(
                    spellEffect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(AnyTarget(chooser = TargetChooser.Opponent)),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.shouldHaveSize(1)
            findings[0].shouldBeInstanceOf<CardValidationError.UnsupportedOpponentChooser>()
                .message shouldContain "opponent's choice"
        }

        it("flags a TargetChooser.Opponent target on a triggered ability") {
            val card = instant(
                "Triggered Opponent-Chosen",
                CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId.generate(),
                            trigger = EventPattern.CastThisSpellEvent,
                            effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            targetRequirement = AnyTarget(chooser = TargetChooser.Opponent),
                        )
                    ),
                ),
            )
            val findings = CardLinter.lint(card)
            findings.filterIsInstance<CardValidationError.UnsupportedOpponentChooser>().shouldHaveSize(1)
        }

        it("accepts a TargetChooser.Opponent target on an activated ability (Cuombajj Witches)") {
            val card = instant(
                "Rightly Opponent-Chosen",
                CardScript(
                    activatedAbilities = listOf(
                        ActivatedAbility(
                            cost = AbilityCost.Tap,
                            effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            targetRequirements = listOf(
                                AnyTarget(),
                                AnyTarget(chooser = TargetChooser.Opponent),
                            ),
                        )
                    ),
                ),
            )
            CardLinter.lint(card).filterIsInstance<CardValidationError.UnsupportedOpponentChooser>()
                .shouldBeEmpty()
        }
    }

    describe("EntityMatches entity roles") {

        it("flags an entity role the ConditionEvaluator doesn't dispatch — a silent constant false") {
            val card = instant(
                "Unsupported Role",
                CardScript(
                    spellEffect = ConditionalEffect(
                        condition = EntityMatches(EffectTarget.Controller, GameObjectFilter.Creature),
                        effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            val findings = CardLinter.lint(card)
                .filterIsInstance<CardValidationError.UnsupportedEntityMatchesRole>()
            findings.shouldHaveSize(1)
            findings[0].message shouldContain "Controller"
        }

        it("accepts the dispatched roles, including EquippedCreature (no facade names it)") {
            val card = instant(
                "Supported Roles",
                CardScript(
                    spellEffect = CompositeEffect(
                        listOf(
                            ConditionalEffect(
                                condition = EntityMatches(EffectTarget.Self, GameObjectFilter.Creature),
                                effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            ),
                            ConditionalEffect(
                                condition = EntityMatches(
                                    EffectTarget.EquippedCreature,
                                    GameObjectFilter.Creature,
                                ),
                                effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            ),
                            ConditionalEffect(
                                condition = EntityMatches(
                                    EffectTarget.ContextTarget(0),
                                    GameObjectFilter.Creature,
                                ),
                                effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            ),
                        ),
                    ),
                    targetRequirements = listOf(AnyTarget()),
                ),
            )
            CardLinter.lint(card)
                .filterIsInstance<CardValidationError.UnsupportedEntityMatchesRole>()
                .shouldBeEmpty()
        }
    }
})
