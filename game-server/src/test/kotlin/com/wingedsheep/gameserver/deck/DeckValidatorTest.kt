package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class DeckValidatorTest : FunSpec({

    val registry = CardRegistry().apply { register(PortalSet.allCards) }
    val validator = DeckValidator(registry)

    test("empty deck reports too-few-cards error") {
        val result = validator.validate(emptyMap())
        result.valid shouldBe false
        result.totalCards shouldBe 0
        result.errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }

    test("legal 40-card mono-mountain deck validates") {
        val deck = mapOf("Mountain" to 40)
        val result = validator.validate(deck)
        result.valid shouldBe true
        result.totalCards shouldBe 40
        result.errors.shouldBeEmpty()
    }

    test("unknown card name produces UNKNOWN_CARD error") {
        val deck = mapOf("Not A Real Card" to 4, "Mountain" to 36)
        val result = validator.validate(deck)
        result.valid shouldBe false
        val unknown = result.errors.single { it.code == "UNKNOWN_CARD" }
        unknown.cardName shouldBe "Not A Real Card"
    }

    test("five copies of a non-basic produces TOO_MANY_COPIES") {
        val deck = mapOf("Bog Imp" to 5, "Mountain" to 35)
        val result = validator.validate(deck)
        result.valid shouldBe false
        val tooMany = result.errors.single { it.code == "TOO_MANY_COPIES" }
        tooMany.cardName shouldBe "Bog Imp"
    }

    test("twenty copies of a basic land is fine") {
        val deck = mapOf("Mountain" to 20, "Forest" to 20)
        val result = validator.validate(deck)
        result.valid shouldBe true
    }

    test("collector-number variants of basics stack toward the same name and stay legal") {
        // Two Plains variants both summing to >4 must NOT trigger TOO_MANY_COPIES because Plains is basic.
        val plainsVariants = registry.getCardsByName("Plains")
        if (plainsVariants.size >= 2) {
            val v1 = plainsVariants[0].metadata.collectorNumber!!
            val v2 = plainsVariants[1].metadata.collectorNumber!!
            val setCode = plainsVariants[0].setCode
            val key1 = if (setCode != null) "Plains#$setCode-$v1" else "Plains#$v1"
            val key2 = if (setCode != null) "Plains#$setCode-$v2" else "Plains#$v2"
            val deck = mapOf(key1 to 20, key2 to 20)
            val result = validator.validate(deck)
            result.valid shouldBe true
        }
    }

    test("zero/negative entries are ignored, not counted toward total") {
        val deck = mapOf("Mountain" to 40, "Forest" to 0)
        val result = validator.validate(deck)
        result.totalCards shouldBe 40
        result.valid shouldBe true
    }

    test("under-40 deck is flagged") {
        val deck = mapOf("Mountain" to 30)
        val result = validator.validate(deck)
        result.valid shouldBe false
        result.errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }
})
