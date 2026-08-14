package com.wingedsheep.engine.limited

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class BoosterGeneratorWithSetsTest : FunSpec({

    fun config(code: String) = BoosterGenerator.SetConfig(
        setCode = code,
        setName = code,
        cards = emptyList(),
        basicLands = emptyList(),
    )

    test("withSets returns a derived generator without mutating the original") {
        val originalConfig = config("OLD")
        val addedConfig = config("NEW")
        val original = BoosterGenerator(mapOf("OLD" to originalConfig))

        val derived = original.withSets(mapOf("NEW" to addedConfig))

        original.availableSets shouldContainExactly mapOf("OLD" to originalConfig)
        original.getSetConfig("NEW").shouldBeNull()
        derived.availableSets shouldContainExactly mapOf(
            "OLD" to originalConfig,
            "NEW" to addedConfig,
        )
    }

    test("extra set replaces the same code only in the derived generator") {
        val originalConfig = config("CUBE").copy(setName = "Old")
        val replacement = config("CUBE").copy(setName = "Lobby Cube")
        val original = BoosterGenerator(mapOf("CUBE" to originalConfig))

        val derived = original.withSets(mapOf("CUBE" to replacement))

        original.getSetConfig("CUBE") shouldBe originalConfig
        derived.getSetConfig("CUBE") shouldBe replacement
    }
})
