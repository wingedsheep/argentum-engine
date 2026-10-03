package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseText
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The three-type list: "artifact, enchantment, or land" is the SDK's flat union constant, and
 * "artifact, enchantment, or creature with flying" is the `or` fold the hand-written cards write —
 * the qualification on the last member is what decides which, so each model has one printed form.
 */
class TypeListTest : StringSpec({

    fun read(text: String): GameObjectFilter =
        Filters.filter.parseText(text).shouldBeInstanceOf<ParseOutcome.Accepted<GameObjectFilter>>().value

    "three bare types are the flat union the SDK publishes" {
        read("artifact, enchantment, or land") shouldBe GameObjectFilter.ArtifactEnchantmentOrLand
        read("artifact, creature, or enchantment") shouldBe GameObjectFilter.ArtifactCreatureOrEnchantment
        read("creature, enchantment, or planeswalker") shouldBe GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(CardPredicate.IsCreature, CardPredicate.IsEnchantment, CardPredicate.IsPlaneswalker),
                ),
            ),
        )
    }

    "a qualified last member is the or fold, and binds to that member alone" {
        read("artifact, enchantment, or creature with flying") shouldBe
            (GameObjectFilter.Artifact or GameObjectFilter.Enchantment or
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
        read("artifact, enchantment, or creature with power 4 or greater") shouldBe
            (GameObjectFilter.Artifact or GameObjectFilter.Enchantment or GameObjectFilter.Creature.powerAtLeast(4))
        read("artifact, enchantment, or tapped creature") shouldBe
            (GameObjectFilter.Artifact or GameObjectFilter.Enchantment or GameObjectFilter.Creature.tapped())
    }

    "every list round-trips" {
        for (text in listOf(
            "artifact, enchantment, or land",
            "artifact, enchantment, or planeswalker",
            "creature, enchantment, or planeswalker",
            "artifact, enchantment, or creature with flying",
            "artifact, enchantment, or creature with power 4 or greater",
            "artifact, enchantment, or tapped creature",
        )) {
            Filters.filter.unparse(read(text)) shouldBe text
        }
    }

    "the other shape of each value refuses to print" {
        // The fold of three bare types, and the flat union with a qualified member.
        Filters.filter.unparse(GameObjectFilter.Artifact or GameObjectFilter.Enchantment or GameObjectFilter.Land) shouldBe
            null
        val flatQualified = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.IsArtifact,
                        CardPredicate.IsEnchantment,
                        CardPredicate.And(listOf(CardPredicate.IsCreature, CardPredicate.HasKeyword(Keyword.FLYING))),
                    ),
                ),
            ),
        )
        Filters.filter.unparse(flatQualified) shouldBe null
    }

    "the list takes its article from its first member" {
        Filters.indefinite.parseText("an artifact, enchantment, or land")
            .shouldBeInstanceOf<ParseOutcome.Accepted<GameObjectFilter>>()
    }
})
