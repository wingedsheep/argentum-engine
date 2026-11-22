package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.sdk.core.PrimordialFormat
import com.wingedsheep.sdk.core.PrimordialFormat.Pool
import com.wingedsheep.sdk.core.PrimordialFormat.Slot

/**
 * Which Primordial pools (blocks / standalone sets) each card is printed in, and at which rarity
 * slot — read off the [PrintingRegistry], so a set's reprint rows count exactly like its own
 * cards. Built once over the whole corpus.
 *
 * A card's slot in a pool is its *lowest* printed rarity across the pool's sets
 * ([PrimordialFormat]: "lowest printed rarity", rules 4.00).
 */
class PrimordialCardPool(printings: PrintingRegistry) {

    private val slotsByName: Map<String, Map<Pool, Slot>> = buildMap<String, MutableMap<Pool, Slot>> {
        for (printing in printings.allPrintings()) {
            val pool = PrimordialFormat.poolOf(printing.setCode) ?: continue
            val slot = Slot.of(printing.rarity)
            val byPool = getOrPut(printing.name) { mutableMapOf() }
            // Slot entries run RARE → COMMON, so the higher ordinal is the lower rarity.
            byPool.merge(pool, slot) { a, b -> if (b.ordinal > a.ordinal) b else a }
        }
    }

    /** The pools [cardName] is printed in, each with the slot it occupies there. */
    fun poolsOf(cardName: String): Map<Pool, Slot> = slotsByName[cardName].orEmpty()

    /**
     * Whether [cardName] can appear in *some* Primordial deck: not banned, and either one of the
     * five free basics or printed in a legal pool.
     */
    fun isLegal(cardName: String): Boolean =
        !PrimordialFormat.isBanned(cardName) &&
            (cardName in PrimordialFormat.FREE_BASIC_LANDS || poolsOf(cardName).isNotEmpty())
}
