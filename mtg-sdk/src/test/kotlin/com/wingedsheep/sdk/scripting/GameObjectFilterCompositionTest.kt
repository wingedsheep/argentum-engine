package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.evaluateWith
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Pins the fail-open closures on [GameObjectFilter.and] and the [ControllerPredicate]
 * combinators (sdk-analysis §1.3): AND-ing two filters used to silently discard the left
 * controller predicate on conflict; heterogeneous controller relationships now compose
 * explicitly via [ControllerPredicate.And]/[ControllerPredicate.Or]/[ControllerPredicate.Not].
 */
class GameObjectFilterCompositionTest : DescribeSpec({

    describe("GameObjectFilter.and controller predicates") {

        it("keeps the predicate when only one side has one") {
            (GameObjectFilter.Creature.youControl() and GameObjectFilter.Artifact)
                .controllerPredicate shouldBe ControllerPredicate.ControlledByYou
            (GameObjectFilter.Creature and GameObjectFilter.Artifact.youControl())
                .controllerPredicate shouldBe ControllerPredicate.ControlledByYou
        }

        it("keeps a predicate both sides share") {
            (GameObjectFilter.Creature.youControl() and GameObjectFilter.Artifact.youControl())
                .controllerPredicate shouldBe ControllerPredicate.ControlledByYou
        }

        it("rejects two different controller predicates instead of silently discarding one") {
            val exception = shouldThrow<IllegalArgumentException> {
                GameObjectFilter.Creature.youControl() and GameObjectFilter.Artifact.opponentControls()
            }
            exception.message shouldContain "withControllerPredicate"
        }

        it("rejects a right-hand side whose anyOf union would be silently dropped") {
            val union = GameObjectFilter.Artifact or GameObjectFilter.Creature.tapped()
            shouldThrow<IllegalArgumentException> {
                GameObjectFilter.Creature and union
            }
        }
    }

    describe("ControllerPredicate combinators") {

        // Leaf semantics for the fold: "owned by you" is true, everything else false.
        val leaf: (ControllerPredicate) -> Boolean = { it == ControllerPredicate.OwnedByYou }

        it("And requires every branch") {
            ControllerPredicate.And(
                listOf(ControllerPredicate.OwnedByYou, ControllerPredicate.ControlledByOpponent)
            ).evaluateWith(leaf) shouldBe false
            ControllerPredicate.And(
                listOf(ControllerPredicate.OwnedByYou, ControllerPredicate.OwnedByYou)
            ).evaluateWith(leaf) shouldBe true
        }

        it("Or requires any branch") {
            ControllerPredicate.Or(
                listOf(ControllerPredicate.ControlledByOpponent, ControllerPredicate.OwnedByYou)
            ).evaluateWith(leaf) shouldBe true
            ControllerPredicate.Or(
                listOf(ControllerPredicate.ControlledByOpponent, ControllerPredicate.ControlledByYou)
            ).evaluateWith(leaf) shouldBe false
        }

        it("Not inverts, including when nested") {
            ControllerPredicate.Not(ControllerPredicate.OwnedByYou).evaluateWith(leaf) shouldBe false
            ControllerPredicate.Not(
                ControllerPredicate.And(
                    listOf(ControllerPredicate.OwnedByYou, ControllerPredicate.ControlledByYou)
                )
            ).evaluateWith(leaf) shouldBe true
        }

        it("survives a JSON round-trip inside a filter") {
            val filter = GameObjectFilter.Permanent.withControllerPredicate(
                ControllerPredicate.And(
                    listOf(
                        ControllerPredicate.OwnedByYou,
                        ControllerPredicate.Not(ControllerPredicate.ControlledByYou),
                    )
                )
            )
            val json = CardSerialization.json.encodeToString(GameObjectFilter.serializer(), filter)
            val decoded = CardSerialization.json.decodeFromString(GameObjectFilter.serializer(), json)
            decoded shouldBe filter
        }

        it("describes composed predicates readably") {
            ControllerPredicate.And(
                listOf(
                    ControllerPredicate.OwnedByYou,
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou),
                )
            ).description shouldBe "you own and not you control"
        }
    }
})
