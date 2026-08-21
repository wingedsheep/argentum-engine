package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
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
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetObject
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

        // The trigger-side choosers are the mirror image: honored on a *triggered* ability and
        // nowhere else. A `TargetObject` carries a chooser too, so the guard can't be anchored on
        // `AnyTarget` alone.
        it("accepts the trigger-side choosers on a triggered ability (Quicksilver Fountain)") {
            val card = instant(
                "Rightly Trigger-Chosen",
                CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId.generate(),
                            trigger = EventPattern.CastThisSpellEvent,
                            effect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                            targetRequirement = TargetObject(
                                filter = TargetFilter.Creature,
                                chooser = TargetChooser.TriggeringPlayer,
                            ),
                        )
                    ),
                ),
            )
            CardLinter.lint(card).filterIsInstance<CardValidationError.UnsupportedOpponentChooser>()
                .shouldBeEmpty()
        }

        it("flags a trigger-side chooser on a spell — nothing would route it") {
            val card = instant(
                "Wrongly Trigger-Chosen",
                CardScript(
                    spellEffect = DealDamageEffect(DynamicAmount.Fixed(1), EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(
                        TargetObject(
                            filter = TargetFilter.Creature,
                            chooser = TargetChooser.ControllerOfTriggeringEntity,
                        )
                    ),
                ),
            )
            val findings = CardLinter.lint(card)
                .filterIsInstance<CardValidationError.UnsupportedOpponentChooser>()
            findings.shouldHaveSize(1)
            findings[0].message shouldContain "a triggered ability"
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

    describe("mana-ability classification (CR 605.1a)") {

        // Chromatic Sphere's shape: "{1}, {T}, Sacrifice this artifact: Add one mana of any color.
        // Draw a card." Mana plus a library move — a mana ability until August 7, 2026, and an
        // ordinary activated ability from then on.
        fun rock(ability: ActivatedAbility) = CardDefinition(
            name = "Classification Rock",
            manaCost = ManaCost.parse("{1}"),
            typeLine = TypeLine.artifact(),
            script = CardScript(activatedAbilities = listOf(ability)),
        )

        fun ability(
            effect: com.wingedsheep.sdk.scripting.effects.Effect,
            cost: AbilityCost = AbilityCost.Tap,
            isManaAbility: Boolean,
        ) = ActivatedAbility(
            id = AbilityId.generate(),
            cost = cost,
            effect = effect,
            isManaAbility = isManaAbility,
        )

        val addGreen = AddManaEffect(Color.GREEN)

        fun misflagged(card: CardDefinition) = CardLinter.lint(card)
            .filterIsInstance<CardValidationError.MisflaggedManaAbility>()

        fun unflagged(card: CardDefinition) = CardLinter.lint(card)
            .filterIsInstance<CardValidationError.UnflaggedManaAbility>()

        it("accepts a plain tap-for-mana ability flagged as one") {
            val card = rock(ability(addGreen, isManaAbility = true))
            misflagged(card).shouldBeEmpty()
            unflagged(card).shouldBeEmpty()
        }

        it("flags a mana ability whose effect draws a card") {
            val found = misflagged(
                rock(
                    ability(
                        CompositeEffect(listOf(addGreen, DrawCardsEffect(1))),
                        isManaAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
            found[0].message shouldContain "moves a card to or from a library"
        }

        // Deranged Assistant: "{T}, Mill a card: Add {C}." The disqualifier is in the *cost*, which
        // is the half of 605.1a's "its cost and effect" a check on the effect alone would miss.
        it("flags a mana ability whose cost mills") {
            val found = misflagged(
                rock(
                    ability(
                        addGreen,
                        cost = AbilityCost.Composite(
                            listOf(AbilityCost.Tap, AbilityCost.Atom(CostAtom.Mill())),
                        ),
                        isManaAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
            found[0].message shouldContain "moves a card to or from a library"
        }

        it("flags a mana ability whose cost exiles from a library") {
            val found = misflagged(
                rock(
                    ability(
                        addGreen,
                        cost = AbilityCost.Atom(CostAtom.ExileFrom(Zone.LIBRARY)),
                        isManaAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
        }

        // The same atom against a graveyard moves nothing to or from a library, so Molt Tender's
        // "exile a card from your graveyard" additional cost leaves it a mana ability.
        it("accepts a mana ability whose cost exiles from a graveyard") {
            misflagged(
                rock(
                    ability(
                        addGreen,
                        cost = AbilityCost.Atom(CostAtom.ExileFrom(Zone.GRAVEYARD)),
                        isManaAbility = true,
                    ),
                ),
            ).shouldBeEmpty()
        }

        // Mill has no serial name of its own — it is a GatherCards(TopOfLibrary) → MoveCollection
        // pipeline — so a check that only knew effect names would miss it entirely.
        it("flags a mana ability that mills as part of its effect") {
            val found = misflagged(
                rock(
                    ability(
                        CompositeEffect(listOf(addGreen, Patterns.Library.mill(1))),
                        isManaAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
            found[0].message shouldContain "moves a card to or from a library"
        }

        it("flags a mana ability that searches its library") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(listOf(addGreen, Patterns.Library.searchLibrary(count = 1))),
                        isManaAbility = true,
                    ),
                ),
            ).shouldHaveSize(1)
        }

        // Scry reorders cards inside the library and moves none to or from it, so Path of Ancestry
        // keeps its classification. This is the assertion that stops the check overreaching.
        it("accepts a mana ability with a scry rider") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(listOf(addGreen, ScryEffect(1))),
                        isManaAbility = true,
                    ),
                ),
            ).shouldBeEmpty()
        }

        // The same reorder written as the pipeline the `Scry` node expands to. Nothing crosses the
        // library boundary — the cards are gathered from the library and put straight back — so the
        // classification must not depend on which spelling the card used.
        it("accepts a mana ability that looks at the top of the library and puts it back") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(listOf(addGreen, Patterns.Library.lookAtTopAndReorder(2))),
                        isManaAbility = true,
                    ),
                ),
            ).shouldBeEmpty()
        }

        // The near neighbour that *does* move a card out: same gather, but one card is kept.
        it("flags a mana ability that looks at the top of the library and keeps a card") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(
                            listOf(
                                addGreen,
                                Patterns.Library.lookAtTopAndKeep(
                                    count = 2,
                                    keepCount = 1,
                                    keepDestination = CardDestination.ToZone(Zone.HAND),
                                ),
                            ),
                        ),
                        isManaAbility = true,
                    ),
                ),
            ).shouldHaveSize(1)
        }

        // The other direction across the boundary: a card that did not come from a library ends up
        // in one.
        it("flags a mana ability that puts a card from hand on top of the library") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(
                            listOf(
                                addGreen,
                                GatherCardsEffect(
                                    source = CardSource.FromZone(Zone.HAND),
                                    storeAs = "tucked",
                                ),
                                MoveCollectionEffect(
                                    from = "tucked",
                                    destination = CardDestination.ToZone(Zone.LIBRARY),
                                ),
                            ),
                        ),
                        isManaAbility = true,
                    ),
                ),
            ).shouldHaveSize(1)
        }

        // A library gather whose cards leave by being *cast* rather than by a move — no destination
        // node says "library", so only the cast marks the crossing.
        it("flags a mana ability that gathers from a library and casts what it found") {
            misflagged(
                rock(
                    ability(
                        CompositeEffect(
                            listOf(
                                addGreen,
                                GatherCardsEffect(
                                    source = CardSource.FromZone(Zone.LIBRARY),
                                    storeAs = "found",
                                ),
                                CastFromCollectionWithoutPayingCostEffect(from = "found"),
                            ),
                        ),
                        isManaAbility = true,
                    ),
                ),
            ).shouldHaveSize(1)
        }

        it("flags a mana ability that is also a loyalty ability") {
            val found = misflagged(
                rock(
                    ActivatedAbility(
                        id = AbilityId.generate(),
                        cost = AbilityCost.Tap,
                        effect = addGreen,
                        isManaAbility = true,
                        isPlaneswalkerAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
            found[0].message shouldContain "it is a loyalty ability"
        }

        it("flags a mana ability that requires a target") {
            val found = misflagged(
                rock(
                    ActivatedAbility(
                        id = AbilityId.generate(),
                        cost = AbilityCost.Tap,
                        effect = addGreen,
                        targetRequirements = listOf(AnyTarget()),
                        isManaAbility = true,
                    ),
                ),
            )
            found.shouldHaveSize(1)
            found[0].message shouldContain "requires a target"
        }

        // The unflagged direction has to honour the same disqualifiers, or reclassifying a card
        // trades one finding for the opposite one and the build can never go green.
        it("does not ask an ability disqualified by its cost to be flagged") {
            unflagged(
                rock(
                    ability(
                        addGreen,
                        cost = AbilityCost.Composite(
                            listOf(AbilityCost.Tap, AbilityCost.Atom(CostAtom.Mill())),
                        ),
                        isManaAbility = false,
                    ),
                ),
            ).shouldBeEmpty()
        }

        it("still asks a plain unflagged tap-for-mana ability to be flagged") {
            unflagged(rock(ability(addGreen, isManaAbility = false))).shouldHaveSize(1)
        }
    }

    describe("attach-scope filters on cards that can't be attached") {

        // GrantWard's filter defaults to GroupFilter.attachedCreature(), so the buggy shape is
        // literally "forgot the second argument" — as Harmonious Grovestrider did.
        fun wardGrant(filter: GroupFilter? = null) =
            if (filter == null) GrantWard(WardCost.Mana("{2}"))
            else GrantWard(WardCost.Mana("{2}"), filter)

        fun beast(script: CardScript, equipCost: ManaCost? = null, back: CardDefinition? = null) =
            CardDefinition(
                name = "Attach Scope Beast",
                manaCost = ManaCost.parse("{3}{G}{G}"),
                typeLine = TypeLine.creature(setOf(Subtype("Beast"))),
                creatureStats = CreatureStats(3, 3),
                script = script,
                equipCost = equipCost,
                backFace = back,
            )

        fun findings(card: CardDefinition) = CardLinter.lint(card)
            .filterIsInstance<CardValidationError.AttachedScopeGrantOnNonAttachment>()

        it("flags a creature whose ward grant kept the default attached-creature filter") {
            val found = findings(beast(CardScript(staticAbilities = listOf(wardGrant()))))
            found.shouldHaveSize(1)
            found[0].message shouldContain "Scope.AttachedTo"
            found[0].message shouldContain "silent no-op"
        }

        it("accepts the same grant on an Aura, where attach scope is the point") {
            val aura = CardDefinition(
                name = "Warding Aura",
                manaCost = ManaCost.parse("{1}{W}"),
                typeLine = TypeLine.aura(),
                script = CardScript(
                    auraTarget = AnyTarget(),
                    staticAbilities = listOf(wardGrant()),
                ),
            )
            findings(aura).shouldBeEmpty()
        }

        it("accepts the same grant on an Equipment") {
            val boots = CardDefinition(
                name = "Warding Boots",
                manaCost = ManaCost.parse("{1}"),
                typeLine = TypeLine.equipment(),
                script = CardScript(staticAbilities = listOf(wardGrant())),
                equipCost = ManaCost.parse("{1}"),
            )
            findings(boots).shouldBeEmpty()
        }

        it("accepts a creature's own printed ward, which is the correct fix") {
            val card = beast(CardScript()).copy(
                keywordAbilities = listOf(KeywordAbility.ward("{2}")),
            )
            findings(card).shouldBeEmpty()
        }

        it("accepts a lord-style grant with an explicit battlefield-scoped filter") {
            val lord = beast(
                CardScript(
                    staticAbilities = listOf(wardGrant(GroupFilter.OtherCreaturesYouControl)),
                ),
            )
            findings(lord).shouldBeEmpty()
        }

        // The Irencrag: a Legendary Artifact whose trigger turns it into an Equipment and grants
        // "equipped creature gets +3/+3". Attach scope inside an effect is correct — by the time
        // the grant applies, the card is an Equipment.
        it("does not flag attach scope nested inside an effect that first makes it an Equipment") {
            val irencrag = CardDefinition(
                name = "Becomes Equipment",
                manaCost = ManaCost.parse("{2}"),
                typeLine = TypeLine.artifact(),
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = AbilityId.generate(),
                            trigger = EventPattern.CastThisSpellEvent,
                            effect = BecomeArtifactEffect(
                                target = EffectTarget.Self,
                                cardTypes = setOf("ARTIFACT"),
                                subtypes = setOf("Equipment"),
                                loseAllAbilities = true,
                                grantedStaticAbilities = listOf(ModifyStats(3, 3)),
                            ),
                        ),
                    ),
                ),
            )
            findings(irencrag).shouldBeEmpty()
        }

        it("does not flag a creature that transforms into an Aura on its back face") {
            val back = CardDefinition(
                name = "Beast Aura Back",
                manaCost = ManaCost.parse("{3}{G}{G}"),
                typeLine = TypeLine.aura(),
                script = CardScript(
                    auraTarget = AnyTarget(),
                    staticAbilities = listOf(wardGrant()),
                ),
            )
            findings(beast(CardScript(), back = back)).shouldBeEmpty()
        }
    }
})
