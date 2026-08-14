package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.limited.BoosterStrategy

/** Builds the synthetic set config that lets existing limited infrastructure describe a cube. */
object CubeSetConfig {
    const val SET_CODE = "CUBE"

    private val unsupportedBoosterStrategy = BoosterStrategy { _, _ ->
        error("Cube packs must be dealt through CubeDealer, not BoosterStrategy")
    }

    fun of(
        cube: ResolvedCube,
        boosterGenerator: BoosterGenerator,
    ): BoosterGenerator.SetConfig {
        val basicLandSource = requireNotNull(
            boosterGenerator.getSetConfig(cube.basicLandSetCode)
        ) {
            "Unknown cube basic-land set code: ${cube.basicLandSetCode}"
        }

        return BoosterGenerator.SetConfig(
            setCode = SET_CODE,
            setName = cube.name,
            cards = cube.cards,
            basicLands = basicLandSource.basicLands,
            incomplete = false,
            sealedSupported = true,
            extensionSet = false,
            boosterStrategy = unsupportedBoosterStrategy,
            variantChance = 0.0,
        )
    }
}
