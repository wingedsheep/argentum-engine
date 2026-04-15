package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.EntityReference
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Covers [PredicateEvaluator.matchesFilter] for [CastSpellRecord] and its private
 * per-predicate dispatcher. These are used for "cast-time retrospective" filters
 * (e.g., "if you've cast two or more instants this turn"), and were 0% covered.
 *
 * Tests construct [CastSpellRecord] values directly and walk through every
 * predicate category the matcher supports — including the "not meaningful for a
 * cast record" branches that must return false, and the composite And/Or/Not wiring.
 */
class PredicateEvaluatorRecordTest : FunSpec({

    val evaluator = PredicateEvaluator()

    fun record(
        typeLine: TypeLine,
        manaValue: Int = 0,
        colors: Set<Color> = emptySet(),
        isFaceDown: Boolean = false
    ) = CastSpellRecord(typeLine, manaValue, colors, isFaceDown)

    fun filter(vararg predicates: CardPredicate, matchAll: Boolean = true) =
        GameObjectFilter(cardPredicates = predicates.toList(), matchAll = matchAll)

    // --- Core filter shape --------------------------------------------------

    context("matchesFilter top-level behavior") {
        test("empty predicate list matches anything") {
            evaluator.matchesFilter(record(TypeLine.instant()), GameObjectFilter.Any) shouldBe true
            // Even face-down records match GameObjectFilter.Any because cardPredicates is empty
            evaluator.matchesFilter(
                record(TypeLine.instant(), isFaceDown = true),
                GameObjectFilter.Any
            ) shouldBe true
        }

        test("face-down cast records never match any non-empty filter") {
            evaluator.matchesFilter(
                record(TypeLine.creature(), isFaceDown = true),
                GameObjectFilter.Creature
            ) shouldBe false
        }

        test("matchAll=true requires every predicate to match (AND)") {
            val rec = record(TypeLine.creature(setOf(Subtype.BIRD)), manaValue = 3)
            // IsCreature AND HasSubtype Bird AND ManaValueAtLeast 3 all hold
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.IsCreature,
                    CardPredicate.HasSubtype(Subtype.BIRD),
                    CardPredicate.ManaValueAtLeast(3)
                )
            ) shouldBe true

            // Adding a predicate that fails makes the whole AND fail
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.IsCreature,
                    CardPredicate.HasSubtype(Subtype.CAT)
                )
            ) shouldBe false
        }

        test("matchAll=false matches when any predicate matches (OR)") {
            val rec = record(TypeLine.instant())
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.IsSorcery,
                    CardPredicate.IsInstant,
                    matchAll = false
                )
            ) shouldBe true

            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.IsSorcery,
                    CardPredicate.IsCreature,
                    matchAll = false
                )
            ) shouldBe false
        }
    }

    // --- Type predicates ----------------------------------------------------

    context("type predicates") {
        test("positive type matches") {
            evaluator.matchesFilter(record(TypeLine.instant()), filter(CardPredicate.IsInstant)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.sorcery()), filter(CardPredicate.IsSorcery)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsCreature)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.artifact()), filter(CardPredicate.IsArtifact)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.enchantment()), filter(CardPredicate.IsEnchantment)) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.basicLand(Subtype("Mountain"))),
                filter(CardPredicate.IsLand)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.basicLand(Subtype("Forest"))),
                filter(CardPredicate.IsBasicLand)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine(cardTypes = setOf(CardType.PLANESWALKER))),
                filter(CardPredicate.IsPlaneswalker)
            ) shouldBe true
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsPermanent)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.instant()), filter(CardPredicate.IsPermanent)) shouldBe false
        }

        test("negation predicates") {
            evaluator.matchesFilter(record(TypeLine.instant()), filter(CardPredicate.IsNonland)) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.basicLand(Subtype("Forest"))),
                filter(CardPredicate.IsNonland)
            ) shouldBe false

            evaluator.matchesFilter(record(TypeLine.instant()), filter(CardPredicate.IsNoncreature)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsNoncreature)) shouldBe false

            evaluator.matchesFilter(
                record(TypeLine.instant()),
                filter(CardPredicate.IsNonenchantment)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.enchantment()),
                filter(CardPredicate.IsNonenchantment)
            ) shouldBe false
        }

        test("token predicates: spells are never tokens") {
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsToken)) shouldBe false
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsNontoken)) shouldBe true
        }

        test("legendary predicates read the supertype set") {
            val legendary = TypeLine(
                supertypes = setOf(Supertype.LEGENDARY),
                cardTypes = setOf(CardType.CREATURE)
            )
            evaluator.matchesFilter(record(legendary), filter(CardPredicate.IsLegendary)) shouldBe true
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsLegendary)) shouldBe false
            evaluator.matchesFilter(record(TypeLine.creature()), filter(CardPredicate.IsNonlegendary)) shouldBe true
            evaluator.matchesFilter(record(legendary), filter(CardPredicate.IsNonlegendary)) shouldBe false
        }
    }

    // --- Color predicates ---------------------------------------------------

    context("color predicates") {
        test("HasColor and NotColor inspect the recorded color set") {
            val blueSpell = record(TypeLine.instant(), colors = setOf(Color.BLUE))
            evaluator.matchesFilter(blueSpell, filter(CardPredicate.HasColor(Color.BLUE))) shouldBe true
            evaluator.matchesFilter(blueSpell, filter(CardPredicate.HasColor(Color.RED))) shouldBe false
            evaluator.matchesFilter(blueSpell, filter(CardPredicate.NotColor(Color.RED))) shouldBe true
            evaluator.matchesFilter(blueSpell, filter(CardPredicate.NotColor(Color.BLUE))) shouldBe false
        }

        test("Colorless, Monocolored, Multicolored") {
            evaluator.matchesFilter(
                record(TypeLine.instant()),
                filter(CardPredicate.IsColorless)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.instant(), colors = setOf(Color.RED)),
                filter(CardPredicate.IsColorless)
            ) shouldBe false

            evaluator.matchesFilter(
                record(TypeLine.instant(), colors = setOf(Color.RED)),
                filter(CardPredicate.IsMonocolored)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.instant(), colors = setOf(Color.RED, Color.GREEN)),
                filter(CardPredicate.IsMonocolored)
            ) shouldBe false

            evaluator.matchesFilter(
                record(TypeLine.instant(), colors = setOf(Color.RED, Color.GREEN)),
                filter(CardPredicate.IsMulticolored)
            ) shouldBe true
            evaluator.matchesFilter(
                record(TypeLine.instant(), colors = setOf(Color.RED)),
                filter(CardPredicate.IsMulticolored)
            ) shouldBe false
        }
    }

    // --- Subtype predicates -------------------------------------------------

    context("subtype predicates") {
        test("HasSubtype / NotSubtype / HasAnyOfSubtypes") {
            val bird = record(TypeLine.creature(setOf(Subtype.BIRD)))
            evaluator.matchesFilter(bird, filter(CardPredicate.HasSubtype(Subtype.BIRD))) shouldBe true
            evaluator.matchesFilter(bird, filter(CardPredicate.HasSubtype(Subtype.CAT))) shouldBe false
            evaluator.matchesFilter(bird, filter(CardPredicate.NotSubtype(Subtype.CAT))) shouldBe true
            evaluator.matchesFilter(bird, filter(CardPredicate.NotSubtype(Subtype.BIRD))) shouldBe false
            evaluator.matchesFilter(
                bird,
                filter(CardPredicate.HasAnyOfSubtypes(listOf(Subtype.CAT, Subtype.BIRD)))
            ) shouldBe true
            evaluator.matchesFilter(
                bird,
                filter(CardPredicate.HasAnyOfSubtypes(listOf(Subtype.CAT, Subtype.DRAGON)))
            ) shouldBe false
        }

        test("HasBasicLandType treats the landType string as a subtype") {
            val forest = record(TypeLine.basicLand(Subtype("Forest")))
            evaluator.matchesFilter(forest, filter(CardPredicate.HasBasicLandType("Forest"))) shouldBe true
            evaluator.matchesFilter(forest, filter(CardPredicate.HasBasicLandType("Mountain"))) shouldBe false
        }
    }

    // --- Mana value predicates ----------------------------------------------

    context("mana value predicates") {
        test("equals / at-most / at-least") {
            val mv3 = record(TypeLine.instant(), manaValue = 3)
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueEquals(3))) shouldBe true
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueEquals(2))) shouldBe false
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueAtMost(3))) shouldBe true
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueAtMost(2))) shouldBe false
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueAtLeast(3))) shouldBe true
            evaluator.matchesFilter(mv3, filter(CardPredicate.ManaValueAtLeast(4))) shouldBe false
        }
    }

    // --- "Not meaningful for a cast record" branches ------------------------

    context("predicates that are not meaningful for a cast record always return false") {
        val rec = record(TypeLine.creature(setOf(Subtype.BIRD)))

        test("power / toughness predicates all return false") {
            evaluator.matchesFilter(rec, filter(CardPredicate.PowerEquals(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.PowerAtMost(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.PowerAtLeast(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.ToughnessEquals(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.ToughnessAtMost(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.ToughnessAtLeast(2))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.PowerOrToughnessAtLeast(2))) shouldBe false
        }

        test("name / keyword / ability predicates return false") {
            evaluator.matchesFilter(rec, filter(CardPredicate.NameEquals("Anything"))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.HasKeyword(Keyword.FLYING))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.NotKeyword(Keyword.FLYING))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.IsActivatedOrTriggeredAbility)) shouldBe false
        }

        test("source-relative and context predicates return false") {
            evaluator.matchesFilter(rec, filter(CardPredicate.NotOfSourceChosenType)) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.SharesCreatureTypeWithSource)) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.SharesCreatureTypeWithTriggeringEntity)) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.HasChosenSubtype)) shouldBe false
            evaluator.matchesFilter(
                rec,
                filter(CardPredicate.SharesCreatureTypeWith(EntityReference.Source))
            ) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.HasSubtypeFromVariable("x"))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.HasSubtypeInStoredList("x"))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.HasSubtypeInEachStoredGroup("x"))) shouldBe false
        }
    }

    // --- Composite And / Or / Not -------------------------------------------

    context("composite predicates") {
        test("And: all sub-predicates must match") {
            val rec = record(TypeLine.creature(setOf(Subtype.BIRD)), manaValue = 2)
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.And(
                        listOf(CardPredicate.IsCreature, CardPredicate.HasSubtype(Subtype.BIRD))
                    )
                )
            ) shouldBe true
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.And(
                        listOf(CardPredicate.IsCreature, CardPredicate.HasSubtype(Subtype.CAT))
                    )
                )
            ) shouldBe false
        }

        test("Or: any sub-predicate matches") {
            val rec = record(TypeLine.instant())
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.Or(listOf(CardPredicate.IsSorcery, CardPredicate.IsInstant))
                )
            ) shouldBe true
            evaluator.matchesFilter(
                rec,
                filter(
                    CardPredicate.Or(listOf(CardPredicate.IsSorcery, CardPredicate.IsCreature))
                )
            ) shouldBe false
        }

        test("Not inverts the wrapped predicate") {
            val rec = record(TypeLine.creature())
            evaluator.matchesFilter(rec, filter(CardPredicate.Not(CardPredicate.IsCreature))) shouldBe false
            evaluator.matchesFilter(rec, filter(CardPredicate.Not(CardPredicate.IsInstant))) shouldBe true
        }
    }
})
