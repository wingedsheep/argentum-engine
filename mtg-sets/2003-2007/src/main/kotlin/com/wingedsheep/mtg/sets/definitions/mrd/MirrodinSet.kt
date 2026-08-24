package com.wingedsheep.mtg.sets.definitions.mrd

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Mirrodin Set (2003)
 *
 * First set in the Mirrodin block. Introduces the artifact-focused metal plane
 * of Mirrodin alongside mechanics like Affinity, Equipment, Imprint, and Entwine.
 *
 * Set Code: MRD
 * Release Date: October 2, 2003
 * Card Count: 306
 */
object MirrodinSet : MtgSet {

    override val code = "MRD"
    override val displayName = "Mirrodin"
    override val releaseDate = "2003-10-02"
    override val block = "Mirrodin"
    // Complete as of Soul Foundry (291/291) — the flag is what gates `fullyImplemented`, so
    // leaving it set would keep a finished set out of the lobby's set picker.

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    /** Mirrodin's own basics: four arts each of the five types, collector numbers 287-306. */
    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Mirrodin predates token *cards* — Scryfall has no `tmrd` set to sync from — so its
     * token art is self-hosted under `web-client/public/images/tokens/` and declared here, the
     * same route Invasion, Apocalypse and Odyssey take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Soldier",
            imageUri = "/images/tokens/mrd-soldier.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Spirit",
            imageUri = "/images/tokens/mrd-spirit.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mrd.cards"
}
