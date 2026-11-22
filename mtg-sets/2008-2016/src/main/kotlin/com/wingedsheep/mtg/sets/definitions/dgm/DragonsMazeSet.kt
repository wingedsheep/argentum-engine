package com.wingedsheep.mtg.sets.definitions.dgm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DedicatedSlot
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.SheetSelector
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragon's Maze
 *
 * The third set in the Return to Ravnica block: the Implicit Maze race, fuse split cards, and
 * the ten guild champions.
 *
 * Set Code: DGM
 * Release Date: May 3, 2013
 */
object DragonsMazeSet : MtgSet {

    override val code = "DGM"
    override val displayName = "Dragon's Maze"
    override val releaseDate = "2013-05-03"
    override val block = "Return to Ravnica"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (35 rares print twice on the sheet,
     * 10 mythics once: the upgrade is 10 in 80), plus a land slot that never holds a basic: one of the
     * ten Guildgates (23 in 242 each), Maze's End (2 in 242) or one of the ten Return to Ravnica /
     * Gatecrash shocklands (1 in 242 each, a shockland in about one pack in 24). The Guildgates and
     * Maze's End open only in that slot, never on the common or rare sheets. The foil that replaced a
     * common in 9 packs in 40 is cosmetic and not modelled, nor is the common sheet's colour
     * balancing. Figures from mtg.wtf/pack/dgm (taw/magic-search-engine).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        DedicatedSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 10.0 / 80),
            slots = listOf(
                DedicatedSlot(
                    sheet = SheetSelector.Named(GUILDGATES + MAZES_END + SHOCKLANDS.keys),
                    rarityWeights = mapOf(Rarity.COMMON to 230.0, Rarity.MYTHIC to 2.0, Rarity.RARE to 10.0),
                ),
            ),
        )
    }

    override val boosterGuestCards: Map<String, String> get() = SHOCKLANDS

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private val GUILDGATES = setOf(
        "Azorius Guildgate", "Boros Guildgate", "Dimir Guildgate", "Golgari Guildgate", "Gruul Guildgate",
        "Izzet Guildgate", "Orzhov Guildgate", "Rakdos Guildgate", "Selesnya Guildgate", "Simic Guildgate",
    )

    private const val MAZES_END = "Maze's End"

    /** The shocklands the land slot opens, keyed to the set whose printing they open as. */
    private val SHOCKLANDS = mapOf(
        "Blood Crypt" to "RTR", "Hallowed Fountain" to "RTR", "Overgrown Tomb" to "RTR",
        "Steam Vents" to "RTR", "Temple Garden" to "RTR",
        "Breeding Pool" to "GTC", "Godless Shrine" to "GTC", "Sacred Foundry" to "GTC",
        "Stomping Ground" to "GTC", "Watery Grave" to "GTC",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dgm.cards"
}
