package com.wingedsheep.engine.loader

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private fun creature(name: String): CardDefinition = CardDefinition(
    name = name,
    manaCost = ManaCost.parse("{1}"),
    typeLine = TypeLine.creature(setOf(Subtype("Wizard"))),
    creatureStats = CreatureStats(1, 1),
)

class SetLoaderTest : FunSpec({

    test("registerSet indexes cards by lowercase set-code + name") {
        val loader = SetLoader()
        val set = CardSet(
            code = "TST",
            name = "Test Set",
            cards = listOf(creature("Alpha One"), creature("Beta Two")),
        )
        loader.registerSet(set)

        loader.getCard("tst-alpha-one").shouldNotBeNull().name shouldBe "Alpha One"
        loader.getCard("tst-beta-two").shouldNotBeNull().name shouldBe "Beta Two"
        loader.cardCount shouldBe 2
        loader.setCount shouldBe 1
    }

    test("getCardByName is case-insensitive and searches across sets") {
        val loader = SetLoader()
        loader.registerSet(CardSet("A", "A", listOf(creature("Red Wizard"))))
        loader.registerSet(CardSet("B", "B", listOf(creature("Blue Mage"))))

        loader.getCardByName("red wizard").shouldNotBeNull().name shouldBe "Red Wizard"
        loader.getCardByName("BLUE MAGE").shouldNotBeNull().name shouldBe "Blue Mage"
        loader.getCardByName("Unknown").shouldBeNull()
    }

    test("getCard returns null for unknown ids") {
        val loader = SetLoader()
        loader.registerSet(CardSet("X", "X", listOf(creature("Known Card"))))
        loader.getCard("x-unknown").shouldBeNull()
    }

    test("getCardsInSet returns only the cards in the requested set") {
        val loader = SetLoader()
        loader.registerSet(CardSet("S1", "Set 1", listOf(creature("A"), creature("B"))))
        loader.registerSet(CardSet("S2", "Set 2", listOf(creature("C"))))

        loader.getCardsInSet("S1").map { it.name } shouldContainExactlyInAnyOrder listOf("A", "B")
        loader.getCardsInSet("S2").map { it.name } shouldContainExactlyInAnyOrder listOf("C")
        loader.getCardsInSet("missing").shouldBeEmpty()
    }

    test("getAllSets / getAllCards surface every registered entry") {
        val loader = SetLoader()
        loader.registerSet(CardSet("S1", "Set 1", listOf(creature("A"))))
        loader.registerSet(CardSet("S2", "Set 2", listOf(creature("B"), creature("C"))))

        loader.getAllSets().map { it.code } shouldContainExactlyInAnyOrder listOf("S1", "S2")
        loader.getAllCards().map { it.name } shouldContainExactlyInAnyOrder listOf("A", "B", "C")
    }

    test("hasSet reports registration state") {
        val loader = SetLoader()
        loader.hasSet("S1").shouldBeFalse()
        loader.registerSet(CardSet("S1", "Set 1", listOf(creature("Card"))))
        loader.hasSet("S1").shouldBeTrue()
        loader.hasSet("other").shouldBeFalse()
    }

    test("clear removes all sets and cards") {
        val loader = SetLoader()
        loader.registerSet(CardSet("S1", "Set 1", listOf(creature("A"), creature("B"))))
        loader.cardCount shouldBe 2

        loader.clear()

        loader.cardCount shouldBe 0
        loader.setCount shouldBe 0
        loader.hasSet("S1").shouldBeFalse()
        loader.getCard("s1-a").shouldBeNull()
    }

    test("loadAll is safe to call when no providers are registered") {
        // The test classpath has no CardSetProvider registered; loadAll should be a no-op.
        val loader = SetLoader()
        loader.loadAll()
        loader.setCount shouldBe 0
        loader.cardCount shouldBe 0
    }

    test("registerSet with multi-word names builds id using kebab-case") {
        val loader = SetLoader()
        loader.registerSet(
            CardSet("MNS", "My New Set", listOf(creature("Thunder Strike Dragon"))),
        )
        loader.getCard("mns-thunder-strike-dragon").shouldNotBeNull().name shouldBe "Thunder Strike Dragon"
    }

    test("registering the same set code twice replaces the previous set") {
        val loader = SetLoader()
        loader.registerSet(CardSet("DUP", "Dup v1", listOf(creature("First"))))
        loader.registerSet(CardSet("DUP", "Dup v2", listOf(creature("Second"))))

        // setCount still 1, but the loadedSets map now points to v2
        loader.setCount shouldBe 1
        loader.getAllSets().single().name shouldBe "Dup v2"
        // Both cards were indexed (map writes don't evict previous ids)
        loader.getCard("dup-first").shouldNotBeNull()
        loader.getCard("dup-second").shouldNotBeNull()
    }
})
