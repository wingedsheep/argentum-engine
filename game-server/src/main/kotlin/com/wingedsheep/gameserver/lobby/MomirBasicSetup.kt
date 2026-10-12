package com.wingedsheep.gameserver.lobby

import com.wingedsheep.ai.engine.deck.Wishboard
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

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
     * Creatures printed with genuinely no mana cost ([CardDefinition.hasNoManaCost]) are left out.
     * Their mana value is 0, so they would otherwise be most of what `{X}` with X=0 flips into, yet
     * they are not cards a player could ever cast (CR 202.1b / CR 118.6 — having no mana cost is an
     * unpayable cost). In the corpus these are the meld results (CR 701.42) — Hanweir, the Writhing
     * Township and friends — defined so their characteristics exist, but never a card anyone owns;
     * the same reason `BoosterGenerator` and `FormatCardPool` drop them. A printed "{0}" is a
     * payable cost, so Ornithopter and friends stay in.
     *
     * Unknown set codes are skipped. The result is exactly what
     * [Format.MomirBasic.eligibleCreatureNames] expects.
     */
    fun creaturePool(setCodes: Collection<String>): List<String> =
        setCodes
            .mapNotNull { MtgSetCatalog.byCode(it) }
            .flatMap { it.cards }
            .filter { it.isCreature && !it.hasNoManaCost }
            .map { it.name }
            .distinct()
            .sorted()

    /**
     * The full Momir pool: every creature card name across every known set, de-duplicated and
     * sorted (same replay-stability contract as [creaturePool]). This is what the format uses —
     * Momir Basic flips creatures from the entire card base, not a per-lobby set scope.
     */
    fun allCreaturePool(): List<String> = creaturePool(MtgSetCatalog.all.map { it.code })

    /**
     * The table's shared *wishboard*: fifteen cards from the whole card base that every seat owns
     * outside the game (CR 400.11a) — what a Learn creature or a wish the avatar rolls reaches for.
     *
     * Momir Basic has no deckbuilding, so it has no sideboard of its own, and without one every
     * Learn creature the avatar flips (Eyetwitch, Pillardrop Rescuer, …) learns nothing. The
     * wishboard leans on Lessons for exactly that reason (see [Wishboard]), and it is rolled
     * **once per game and shared** by every seat: Momir's randomness belongs in the avatar's
     * flips, not in one player starting with a better menu than the other.
     */
    fun wishboard(random: Random = Random.Default): Map<String, Int> =
        Wishboard.build(MtgSetCatalog.all.flatMap { it.cards }, random)

    /** Build a [Format.MomirBasic] whose pool is every creature across all sets. */
    fun format(): Format.MomirBasic =
        Format.MomirBasic(eligibleCreatureNames = allCreaturePool())
}
