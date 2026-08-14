package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry

/**
 * Resolves user-authored names and optional printing pins against the authoritative registries.
 *
 * Resolution is all-or-nothing: callers receive every miss and no partial playable cube.
 */
class CubeResolver(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
) {
    fun resolve(cube: CubeList): CubeResolution {
        val unresolved = mutableListOf<UnresolvedCubeCard>()
        val resolvedCards = buildList {
            for (entry in cube.cards) {
                val canonical = cardRegistry.getCard(entry.name)
                if (canonical == null) {
                    unresolved += UnresolvedCubeCard(entry.name, "Card is not implemented")
                    continue
                }

                val resolved = entry.printing?.let { ref ->
                    val printing = printingRegistry.getPrinting(ref)
                    when {
                        printing == null -> {
                            unresolved += UnresolvedCubeCard(
                                entry.name,
                                "Printing ${ref.identifier()} is not available",
                            )
                            null
                        }
                        printing.name != canonical.name -> {
                            unresolved += UnresolvedCubeCard(
                                entry.name,
                                "Printing ${ref.identifier()} belongs to ${printing.name}",
                            )
                            null
                        }
                        else -> canonical.withPrinting(printing)
                    }
                } ?: if (entry.printing == null) canonical else null

                if (resolved != null) {
                    repeat(entry.count) { add(resolved) }
                }
            }
        }

        if (unresolved.isNotEmpty()) return CubeResolution.Failure(unresolved)
        return CubeResolution.Success(
            ResolvedCube(
                name = cube.name,
                cards = resolvedCards,
                basicLandSetCode = cube.basicLandSetCode,
                packSize = cube.packSize,
            )
        )
    }
}

sealed interface CubeResolution {
    data class Success(val cube: ResolvedCube) : CubeResolution
    data class Failure(val unresolved: List<UnresolvedCubeCard>) : CubeResolution
}

data class UnresolvedCubeCard(
    val name: String,
    val reason: String,
)
