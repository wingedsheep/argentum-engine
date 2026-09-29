package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BestowSerializationTest : StringSpec({
    "bestow retains fixed and variable mana prices through polymorphic serialization" {
        for (cost in listOf("{3}{G}", "{X}{G}", "{0}")) {
            val original = KeywordAbility.bestow(cost)
            val restored = CardSerialization.json.decodeFromString(
                KeywordAbility.serializer(),
                CardSerialization.json.encodeToString(KeywordAbility.serializer(), original)
            )
            restored shouldBe original
            restored.keyword shouldBe Keyword.BESTOW
            (restored as KeywordAbility.Bestow).cost shouldBe ManaCost.parse(cost)
            restored.additionalCost shouldBe null
        }
    }

    "bestow retains a nonmana payment as part of its alternative price" {
        val payment = Costs.additional.PayLife(2)
        val original = KeywordAbility.bestow("{G}", payment)
        val restored = CardSerialization.json.decodeFromString(
            KeywordAbility.serializer(),
            CardSerialization.json.encodeToString(KeywordAbility.serializer(), original)
        )
        restored shouldBe original
        restored.keyword shouldBe Keyword.BESTOW
        (restored as KeywordAbility.Bestow).additionalCost shouldBe payment
        restored.cost shouldBe ManaCost.parse("{G}")
        restored.description shouldBe "Bestow {G}, Pay 2 life"
    }
})
