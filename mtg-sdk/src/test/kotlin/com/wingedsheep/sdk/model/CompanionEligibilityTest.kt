package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The deck-construction half of companion (CR 702.139a/b): each condition shape read against the
 * printed cards of a starting deck, as the server's deck validator asks it.
 */
class CompanionEligibilityTest : FunSpec({

    fun companion(condition: CompanionCondition) = CardDefinition.creature(
        name = "Test Companion",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Cat")),
        power = 3,
        toughness = 3,
        supertypes = setOf(Supertype.LEGENDARY),
    ).copy(keywordAbilities = listOf(KeywordAbility.companion(condition)))

    fun creature(name: String, cost: String, vararg subtypes: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = subtypes.map(::Subtype).toSet(),
        power = 2,
        toughness = 2,
    )

    val forest = CardDefinition.basicLand("Forest", Subtype("Forest"))
    val bolt = CardDefinition.instant("Bolt", ManaCost.parse("{R}"), "Deal 3.")
    val opt = CardDefinition.instant("Opt", ManaCost.parse("{U}"), "Scry 1, draw.")
    val bear = creature("Bear", "{1}{G}", "Bear")
    val giant = creature("Giant", "{4}{R}", "Giant")
    val cat = creature("Cat", "{1}{W}", "Cat")
    val fireball = CardDefinition.sorcery("Fireball", ManaCost.parse("{X}{R}"), "X damage.")
    val artifactCreature = card("Construct") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Construct"
        power = 2
        toughness = 2
    }
    val rock = CardDefinition.artifact("Rock", ManaCost.parse("{2}"))

    val nonpermanent = GameObjectFilter.Any.withCardPredicate(CardPredicate.Not(CardPredicate.IsPermanent))

    test("a card without a companion ability is no companion") {
        CompanionEligibility.hasCompanion(bear) shouldBe false
        CompanionEligibility.conditionError(bear, listOf(forest), 40).shouldNotBeNull()
    }

    test("EachCard: Lurrus — every permanent card has mana value 2 or less; instants and sorceries are free") {
        val lurrus = companion(CompanionCondition.EachCard(nonpermanent or GameObjectFilter.Permanent.manaValueAtMost(2)))
        CompanionEligibility.conditionError(lurrus, listOf(forest, bear, cat, bolt, fireball), 40).shouldBeNull()
        CompanionEligibility.conditionError(lurrus, listOf(forest, bear, giant), 40)
            .shouldNotBeNull() shouldContain "Giant"
    }

    test("EachCard: Gyruda — even mana values only; a land's 0 is even and X counts as 0") {
        val gyruda = companion(CompanionCondition.EachCard(GameObjectFilter.Any.manaValueIsEven()))
        CompanionEligibility.conditionError(gyruda, listOf(forest, bear, rock), 40).shouldBeNull()
        // Fireball is {X}{R}: mana value 1 off the stack — odd.
        CompanionEligibility.conditionError(gyruda, listOf(forest, fireball), 40).shouldNotBeNull()
    }

    test("EachCard: Obosh — odd mana values and land cards") {
        val obosh = companion(CompanionCondition.EachCard(GameObjectFilter.Land or GameObjectFilter.Any.manaValueIsOdd()))
        CompanionEligibility.conditionError(obosh, listOf(forest, bolt, giant, fireball), 40).shouldBeNull()
        CompanionEligibility.conditionError(obosh, listOf(forest, bear), 40).shouldNotBeNull()
    }

    test("EachCard: Kaheera — creature cards must have one of the types; noncreatures are free") {
        val kaheera = companion(
            CompanionCondition.EachCard(
                GameObjectFilter.Noncreature or GameObjectFilter.Any.withAnyOfSubtypes(listOf(Subtype("Cat"), Subtype("Beast")))
            )
        )
        CompanionEligibility.conditionError(kaheera, listOf(forest, cat, bolt, rock), 40).shouldBeNull()
        CompanionEligibility.conditionError(kaheera, listOf(cat, bear), 40).shouldNotBeNull() shouldContain "Bear"
    }

    test("EachCard: Zirda — permanents need an activated ability; a basic land's intrinsic mana ability counts") {
        val zirda = companion(
            CompanionCondition.EachCard(nonpermanent or GameObjectFilter.Any.withCardPredicate(CardPredicate.HasActivatedAbility))
        )
        val tapper = card("Tapper") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.DrawCards(1)
            }
        }
        CompanionEligibility.conditionError(zirda, listOf(forest, tapper, bolt), 40).shouldBeNull()
        CompanionEligibility.conditionError(zirda, listOf(forest, bear), 40).shouldNotBeNull()
    }

    test("DeckSizeAboveMinimum: Yorion — twenty over the minimum; never in a 100-card Commander deck") {
        val yorion = companion(CompanionCondition.DeckSizeAboveMinimum(20))
        CompanionEligibility.conditionError(yorion, List(60) { forest }, 40).shouldBeNull()
        CompanionEligibility.conditionError(yorion, List(59) { forest }, 40).shouldNotBeNull()
        CompanionEligibility.conditionError(yorion, List(100) { forest }, 100).shouldNotBeNull()
    }

    test("ShareACardType: Umori — one card type common to every nonland card") {
        val umori = companion(CompanionCondition.ShareACardType(GameObjectFilter.Nonland))
        // All creatures, some of them artifact creatures: creature is shared.
        CompanionEligibility.conditionError(umori, listOf(forest, bear, artifactCreature, giant), 40).shouldBeNull()
        // An artifact creature, an artifact and a creature share no single type (ruling).
        CompanionEligibility.conditionError(umori, listOf(artifactCreature, rock, bear), 40).shouldNotBeNull()
        // Lands don't count.
        CompanionEligibility.conditionError(umori, listOf(forest, bolt, opt), 40).shouldBeNull()
    }

    test("DistinctNames: Lutri — no two nonland cards share a name; basic lands may repeat") {
        val lutri = companion(CompanionCondition.DistinctNames(GameObjectFilter.Nonland))
        CompanionEligibility.conditionError(lutri, listOf(forest, forest, bear, bolt), 40).shouldBeNull()
        CompanionEligibility.conditionError(lutri, listOf(bear, bear), 40).shouldNotBeNull() shouldContain "Bear"
    }
})
