package com.wingedsheep.gameserver.cube

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.PrintingRef

/**
 * User-authored cube definition. Entries retain counts because singleton is conventional, not a
 * rule, and may pin a printing for presentation without changing the card's oracle identity.
 */
data class CubeList(
    val name: String,
    val cards: List<CubeCardEntry>,
    val basicLandSetCode: String,
    val packSize: Int = DEFAULT_PACK_SIZE,
) {
    init {
        require(name.isNotBlank()) { "Cube name must not be blank" }
        require(packSize > 0) { "Cube pack size must be positive, got $packSize" }
    }

    companion object {
        const val DEFAULT_PACK_SIZE = 15
    }
}

data class CubeCardEntry(
    val name: String,
    val count: Int = 1,
    val printing: PrintingRef? = null,
) {
    init {
        require(name.isNotBlank()) { "Cube card name must not be blank" }
        require(count > 0) { "Cube card count must be positive for $name, got $count" }
    }
}

/** A fully resolved, playable cube. [cards] is expanded according to each entry's count. */
data class ResolvedCube(
    val name: String,
    val cards: List<CardDefinition>,
    val basicLandSetCode: String,
    val packSize: Int,
)
