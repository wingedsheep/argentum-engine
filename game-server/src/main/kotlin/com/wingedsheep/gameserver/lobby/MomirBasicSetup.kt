package com.wingedsheep.gameserver.lobby

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Format

/**
 * Server-side setup helpers for the Momir Basic Vanguard format ([Format.MomirBasic]).
 *
 * Momir Basic has no deckbuilding: every seat plays the same fixed 60 basic lands and starts with
 * the avatar in the command zone. There is no per-match configuration — the random-creature pool is
 * every creature across all sets. This object translates the catalog into the pre-sorted
 * [Format.MomirBasic.eligibleCreatureNames] the engine treats as opaque, replay-stable data.
 * Keeping the computation here (game-server, which already owns the [MtgSetCatalog] → `CardRegistry`
 * wiring) keeps the engine a pure function of `GameState`.
 */
object MomirBasicSetup {

    /** The five basic land names. */
    val BASIC_LAND_NAMES: List<String> = listOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /** Copies of each basic in the fixed deck (`5 * 12 = 60`). */
    const val COPIES_PER_BASIC: Int = 12

    /**
     * The fixed 60-card deck every Momir Basic seat plays: 12 of each basic land. Identical for
     * both players — there is no deckbuilding in this format.
     */
    val fixedBasicDeck: Map<String, Int> =
        BASIC_LAND_NAMES.associateWith { COPIES_PER_BASIC }

    /**
     * The creature pool the avatar's random copy is drawn from: every creature card name printed in
     * the given [setCodes], de-duplicated by name and **sorted** so the engine's seeded
     * `GameRng.pick` is replay-stable (it filters this list by mana value and picks, never
     * re-collecting from the card registry's unspecified map order).
     *
     * Unknown set codes are skipped. The result is exactly what
     * [Format.MomirBasic.eligibleCreatureNames] expects.
     */
    fun creaturePool(setCodes: Collection<String>): List<String> =
        setCodes
            .mapNotNull { MtgSetCatalog.byCode(it) }
            .flatMap { it.cards }
            .filter { it.isCreature }
            .map { it.name }
            .distinct()
            .sorted()

    /**
     * The full Momir pool: every creature card name across every known set, de-duplicated and
     * sorted (same replay-stability contract as [creaturePool]). This is what the format uses —
     * Momir Basic flips creatures from the entire card base, not a per-lobby set scope.
     */
    fun allCreaturePool(): List<String> = creaturePool(MtgSetCatalog.all.map { it.code })

    /** Build a [Format.MomirBasic] whose pool is every creature across all sets. */
    fun format(): Format.MomirBasic =
        Format.MomirBasic(eligibleCreatureNames = allCreaturePool())
}
