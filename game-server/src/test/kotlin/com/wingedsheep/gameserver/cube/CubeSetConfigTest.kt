package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class CubeSetConfigTest : FunSpec({

    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val cubeCard = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
    )
    val landSource = BoosterGenerator.SetConfig(
        setCode = "BLB",
        setName = "Bloomburrow",
        cards = emptyList(),
        basicLands = listOf(forest),
    )
    val generator = BoosterGenerator(mapOf("BLB" to landSource))
    val cube = ResolvedCube(
        name = "Friends Cube",
        cards = listOf(cubeCard),
        basicLandSetCode = "BLB",
        packSize = 15,
    )

    test("builds a complete synthetic set with basic lands from the selected source") {
        val config = CubeSetConfig.of(cube, generator)

        config.setCode shouldBe "CUBE"
        config.setName shouldBe "Friends Cube"
        config.cards shouldContainExactly listOf(cubeCard)
        config.basicLands shouldContainExactly listOf(forest)
        config.sealedSupported shouldBe true
        config.incomplete shouldBe false
        config.extensionSet shouldBe false
        config.variantChance.shouldBeExactly(0.0)
    }

    test("synthetic booster strategy fails if a caller bypasses CubeDealer") {
        val config = CubeSetConfig.of(cube, generator)

        val error = shouldThrow<IllegalStateException> {
            config.boosterStrategy.generate(config.cards, Random(1))
        }

        error.message shouldBe
            "Cube packs must be dealt through CubeDealer, not BoosterStrategy"
    }

    test("unknown basic land source fails with a host-facing message") {
        val error = shouldThrow<IllegalArgumentException> {
            CubeSetConfig.of(cube.copy(basicLandSetCode = "NOPE"), generator)
        }

        error.message shouldBe "Unknown cube basic-land set code: NOPE"
    }
})
