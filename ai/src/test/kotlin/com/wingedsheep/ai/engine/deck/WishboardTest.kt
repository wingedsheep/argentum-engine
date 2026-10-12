package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class WishboardTest : FunSpec({

    val cost = ManaCost.parse("{1}{R}")
    fun lesson(name: String) = CardDefinition.sorcery(name, cost, "").copy(
        typeLine = TypeLine(cardTypes = setOf(CardType.SORCERY), subtypes = setOf(Subtype.LESSON)),
    )
    fun instant(name: String) = CardDefinition.instant(name, cost, "")
    fun sorcery(name: String) = CardDefinition.sorcery(name, cost, "")
    fun creature(name: String) = CardDefinition.creature(name, cost, emptySet(), 2, 2)
    fun artifact(name: String) = CardDefinition.artifact(name, cost)

    val creatures = (1..40).map { creature("Bear $it") }
    val lessons = (1..5).map { lesson("Lesson $it") }
    val instants = (1..5).map { instant("Instant $it") }
    val sorceries = (1..5).map { sorcery("Sorcery $it") }

    test("fills fifteen distinct cards, a menu rather than a pile") {
        val board = Wishboard.build(creatures + creatures, Random(1))

        board.size shouldBe Wishboard.SIZE
        board.values.toSet() shouldBe setOf(1)
    }

    test("reserves slots for Lessons and each type a wish names, even when they are rare") {
        // 40 creatures drown out 15 noncreatures in a plain random draw; the slots still find them.
        repeat(20) { seed ->
            val board = Wishboard.build(creatures + lessons + instants + sorceries, Random(seed))

            // At least: the leftover slots are filled at random and can land on any of these, and
            // a Lesson (itself a sorcery) can fill the sorcery slot.
            board.keys.count { it.startsWith("Lesson") } shouldBeGreaterThanOrEqual 3
            board.keys.count { it.startsWith("Instant") } shouldBeGreaterThanOrEqual 2
            board.keys.count { it.startsWith("Lesson") || it.startsWith("Sorcery") } shouldBeGreaterThanOrEqual 5
            board.size shouldBe Wishboard.SIZE
        }
    }

    test("an unfillable slot is skipped and the rest is filled at random") {
        val board = Wishboard.build(creatures + listOf(artifact("Mox")), Random(3))

        board.size shouldBe Wishboard.SIZE
        board.keys shouldContainAll listOf("Mox")
    }

    test("never offers basic lands, meld results, an excluded card, or a nonland with no mana cost") {
        val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
        val meld = creature("Meld Result").copy(meldResult = true)
        val evermind = instant("Evermind").copy(hasNoManaCost = true)
        val inDeck = creature("Played Bear")

        Wishboard.build(listOf(forest, meld, evermind, inDeck), exclude = setOf("Played Bear")).shouldBeEmpty()
    }

    test("a pool smaller than fifteen yields what it has") {
        Wishboard.build(lessons, Random(4)).keys shouldBe lessons.map { it.name }.toSet()
    }
})
