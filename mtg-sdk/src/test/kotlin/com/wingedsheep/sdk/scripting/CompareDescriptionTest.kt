package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * [Compare] descriptions surface directly to players — a `CantAttackUnless` restriction renders its
 * condition into the "you can't attack with that" message, and trigger/activation conditions render
 * into ability text. They must read as English, not as a debug dump of the underlying comparison.
 */
class CompareDescriptionTest : DescribeSpec({

    describe("ComparisonOperator.phrase") {

        it("renders every operator as an English comparative that follows an 'is'") {
            ComparisonOperator.LT.phrase shouldBe "less than"
            ComparisonOperator.LTE.phrase shouldBe "at most"
            ComparisonOperator.EQ.phrase shouldBe "exactly"
            ComparisonOperator.NEQ.phrase shouldBe "not"
            ComparisonOperator.GT.phrase shouldBe "greater than"
            ComparisonOperator.GTE.phrase shouldBe "at least"
        }

        it("keeps the mathematical symbols available for diagnostics") {
            ComparisonOperator.GTE.symbol shouldBe ">="
            ComparisonOperator.LT.symbol shouldBe "<"
        }
    }

    describe("Compare.description") {

        it("reads as a sentence rather than an operator dump") {
            Conditions.YouControlAtLeast(3, GameObjectFilter.Creature).description shouldBe
                "the number of creatures you control is at least 3"
        }

        it("compares two dynamic amounts without leaking a symbol (Goblin Goon)") {
            val moreCreaturesThanDefender = Compare(
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
                ComparisonOperator.GT,
                DynamicAmount.AggregateBattlefield(Player.DefendingPlayer, GameObjectFilter.Creature)
            )
            moreCreaturesThanDefender.description shouldBe
                "the number of creatures you control is greater than " +
                "the number of creatures defending player controls"
        }

        it("renders the whole CantAttackUnless message for Chief Warg's Company") {
            val twoOtherWolves = Compare(
                DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.withSubtype(Subtype.WOLF),
                    excludeSelf = true
                ),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(2)
            )
            val message = CantAttackUnless(twoOtherWolves).description
            message shouldBe "can't attack unless the number of other Wolf creatures you control is at least 2"
            message shouldNotContain ">="
        }
    }

    describe("filter descriptions feeding those comparisons") {

        it("templates the subtype ahead of the card type, as Magic does") {
            GameObjectFilter.Creature.withSubtype(Subtype.WOLF).description shouldBe "Wolf creature"
            GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).description shouldBe
                "Equipment artifact"
        }

        it("keeps the controller qualifier in front of the type words") {
            GameObjectFilter.Creature.withSubtype(Subtype.WOLF).youControl().description shouldBe
                "you control Wolf creature"
        }

        it("agrees the indefinite article with the word the description now leads on") {
            // The article is rendered immediately before the description (CostAtom's
            // "sacrifice ... from <article> <description>"), so hoisting the subtype has to move
            // the article with it — "an Elf creature", never "a Elf creature".
            val elf = GameObjectFilter.Creature.withSubtype(Subtype.ELF)
            elf.indefiniteArticle shouldBe "an"

            val wolf = GameObjectFilter.Creature.withSubtype(Subtype.WOLF)
            wolf.indefiniteArticle shouldBe "a"

            // No subtype to hoist: still keyed off the card type.
            GameObjectFilter.Artifact.indefiniteArticle shouldBe "an"
            GameObjectFilter.Creature.indefiniteArticle shouldBe "a"
        }
    }

    describe("pluralization of counted filters") {

        it("uses -ves for the f-ending creature types Magic actually prints") {
            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Creature.withSubtype(Subtype.WOLF)
            ).description shouldBe "the number of Wolf creatures you control"

            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Any.withSubtype(Subtype.DWARF)
            ).description shouldBe "the number of Dwarves you control"

            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Any.withSubtype(Subtype.ELF)
            ).description shouldBe "the number of Elves you control"
        }

        it("uses -es after a sibilant") {
            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Any.withSubtype(Subtype("Leech"))
            ).description shouldBe "the number of Leeches you control"
        }

        it("still handles the plain and -y cases") {
            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Creature
            ).description shouldBe "the number of creatures you control"

            DynamicAmount.AggregateBattlefield(
                Player.You, GameObjectFilter.Sorcery
            ).description shouldBe "the number of sorceries you control"
        }
    }
})
