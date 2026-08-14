package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class CubeDealerTest : FunSpec({

    fun card(index: Int) = CardDefinition.creature(
        name = "Cube Card $index",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    ).copy(oracleId = "oracle-$index")

    val cube = (1..45).map(::card)

    test("deals exact pack sizes without repeating an identity across packs") {
        val dealer = CubeDealer(cube, packSize = 15, seed = 42)

        val packs = dealer.deal(3)

        packs.map { it.size } shouldContainExactly listOf(15, 15, 15)
        packs.flatten().map { it.oracleId }.distinct().size shouldBeExactly 45
        dealer.remaining shouldBeExactly 0
    }

    test("successive deals consume the same one-time shuffle") {
        val dealer = CubeDealer(cube, packSize = 5, seed = 123)

        val first = dealer.deal(2)
        val second = dealer.deal(1)

        (first + second).flatten().map { it.oracleId }.distinct().size shouldBeExactly 15
        dealer.remaining shouldBeExactly 30
    }

    test("same seed produces the same deal") {
        val first = CubeDealer(cube, packSize = 9, seed = 8675309).deal(4)
        val second = CubeDealer(cube, packSize = 9, seed = 8675309).deal(4)

        first.map { pack -> pack.map { it.oracleId } } shouldBe
            second.map { pack -> pack.map { it.oracleId } }
    }

    test("dealing past capacity fails loudly with the shortfall and consumes nothing") {
        val dealer = CubeDealer(cube.take(20), packSize = 15, seed = 1)

        val error = shouldThrow<IllegalArgumentException> {
            dealer.deal(2)
        }

        error.message shouldBe
            "Cannot deal 2 cube packs of 15 cards: 20 cards remain, short by 10"
        dealer.remaining shouldBeExactly 20
    }

    test("dealing zero packs is a no-op") {
        val dealer = CubeDealer(cube, packSize = 15, seed = 1)

        dealer.deal(0) shouldBe emptyList()
        dealer.remaining shouldBeExactly cube.size
    }

    test("resumes from the ordered undealt tail without reshuffling") {
        val dealer = CubeDealer(cube, packSize = 3, seed = 42L)
        val firstPack = dealer.deal(1).single()
        val remaining = dealer.remainingCards()

        val resumed = CubeDealer.resume(remaining, packSize = 3)
        val resumedPacks = resumed.deal(2).flatten()

        (firstPack + resumedPacks).map { it.name }.toSet().size shouldBe 9
        resumedPacks shouldBe remaining.take(6)
    }
})
